package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.http.Method
import com.twitter.finatra.http.exceptions.MethodNotAllowedException
import com.twitter.finatra.http.AnyMethod
import scala.annotation.tailrec
import scala.collection.mutable.{ArrayBuffer, AnyRefMap => AMap, LinkedHashMap => LMap}

private[http] case class RouteAndParameter(route: Route, routeParams: Map[String, String])
private[http] case class MatchedNonConstantRoute(
  routeAndParamOpt: Option[RouteAndParameter] = None,
  methodNotAllowed: Boolean = false)

private[http] case class TrieNode(
  segment: String,
  constantSegment: Boolean = true,
  // only populated in nodes that represent the last segment of a route
  // use AnyRefMap for faster look ups
  routes: AMap[String, Route] = AMap.empty,
  // only populated in nodes that represent the last segment of a route
  pattern: ArrayBuffer[PathPattern] = new ArrayBuffer[PathPattern](1)) {

  // use a LinkedHashMap to reserve insertion order
  private[http] val children: LMap[String, TrieNode] = new LMap[String, TrieNode]()
}

private[http] class Trie(routes: Seq[Route]) {

  private[this] val root: TrieNode = TrieNode("")

  routes.foreach { route =>
    appendHelper(root, route, 1)
  }

  def find(path: String, method: Method): Option[RouteAndParameter] = {
    val matchedNonConstantRoute = findNonConstantRoute(this.root, method, path, 1)

    // Because we won't stop traversing the trie until we found a match, for non-constant route, a
    // match means routeAndParamOpt is defined. So it might happen when we encountered a methodNotAllowed
    // error first, then found a match later on. So just checking if methodNotAllowed is true here
    // won't be enough.
    if (matchedNonConstantRoute.routeAndParamOpt.isEmpty && matchedNonConstantRoute.methodNotAllowed) {
      throw new MethodNotAllowedException(
        error = "The method " + method + " is not allowed on path " + path)
    }
    matchedNonConstantRoute.routeAndParamOpt
  }

  /* ----------------------------------------- *
   *               Util Methods
   * ----------------------------------------- */

  private[this] def findNonConstantRoute(
    node: TrieNode,
    method: Method,
    path: String,
    startIndex: Int
  ): MatchedNonConstantRoute = {
    val nextIndex = path.indexOf('/', startIndex)
    if (startIndex == 0 || startIndex >= path.length) {
      if (node.routes.isEmpty) {
        MatchedNonConstantRoute()
      } else {
        node.routes.get(method.name).orElse(node.routes.get(AnyMethod.name)) match {
          case Some(route) =>
            if (isMatchedRoute(node, route, path)) {
              val incomingPath = toMatchPath(route.hasOptionalTrailingSlash, path)
              var matchedParams: Map[String, String] = Map.empty[String, String]
              node.pattern.head.extract(incomingPath).foreach(params => matchedParams = params)
              if (matchedParams.isEmpty) {
                MatchedNonConstantRoute()
              } else {
                val routeAndParameter = Some(RouteAndParameter(route, matchedParams))
                MatchedNonConstantRoute(routeAndParameter)
              }
            } else {
              MatchedNonConstantRoute()
            }
          case _ => MatchedNonConstantRoute(None, methodNotAllowed = true)
        }
      }
    } else {
      val currentSegment =
        if (nextIndex == -1) path.substring(startIndex, path.length)
        else path.substring(startIndex, nextIndex)

      /**
       * We look for a match from non-constant routes based on insertion order
       */
      var result: MatchedNonConstantRoute = MatchedNonConstantRoute()
      var methodNotAllowed: Boolean = false
      val childIterator = node.children.valuesIterator
      while (childIterator.hasNext) {
        val child = childIterator.next()
        if (result.routeAndParamOpt.isEmpty) {
          if (child.constantSegment) {
            if (child.segment.equals(currentSegment)) {
              result = findNonConstantRoute(child, method, path, nextIndex + 1)
              methodNotAllowed = methodNotAllowed || result.methodNotAllowed
            }
          } else if (child.segment.last == '*') {
            result = findNonConstantRoute(child, method, path, 0)
            methodNotAllowed = methodNotAllowed || result.methodNotAllowed
          } else {
            result = findNonConstantRoute(child, method, path, nextIndex + 1)
            methodNotAllowed = methodNotAllowed || result.methodNotAllowed
          }
        }
      }
      MatchedNonConstantRoute(result.routeAndParamOpt, methodNotAllowed)
    }
  }

  @tailrec private[this] def appendHelper(node: TrieNode, route: Route, startIndex: Int): Unit = {
    val path = route.path
    val nextIndex = path.indexOf('/', startIndex)
    if (startIndex == 0 || startIndex >= path.length) {
      node.routes.update(route.method.name, route)
      node.pattern += PathPattern(path)
    } else {
      val segment =
        if (nextIndex == -1) path.substring(startIndex, path.length)
        else path.substring(startIndex, nextIndex)
      if (segment == ":*") {
        // update the current node
        node.routes.update(route.method.name, route)
        node.pattern += PathPattern(path)
        // update child node
        node.children.get("*") match {
          case Some(child) =>
            child.routes.update(route.method.name, route)
          case _ =>
            node.children.update(
              "*",
              TrieNode(
                segment = "*",
                constantSegment = false,
                routes = AMap[String, Route](route.method.name -> route),
                pattern = ArrayBuffer[PathPattern](PathPattern(path))
              )
            )
        }
      } else {
        val isConstantSegment = segment == "" || !segment.contains(':')
        val key = if (isConstantSegment) segment else segment.substring(1)

        val child = node.children.getOrElseUpdate(
          key,
          TrieNode(segment = key, constantSegment = isConstantSegment)
        )
        appendHelper(child, route, nextIndex + 1)
      }
    }
  }

  private[this] def isMatchedRoute(node: TrieNode, route: Route, path: String): Boolean = {
    val storedPathLast = route.path.last
    val pathHasTrailingSlash = route.hasOptionalTrailingSlash || path.last == '/'
    val routeHasTrailingSlash = storedPathLast == '/'
    val matchedTrailingSlash = !(pathHasTrailingSlash ^ routeHasTrailingSlash)
    matchedTrailingSlash || storedPathLast == '*'
  }

  /** routes are stored with the optional trailing slash, thus we add it to match if not present */
  private[this] def toMatchPath(hasOptionalTrailingSlash: Boolean, incomingPath: String): String = {
    if (hasOptionalTrailingSlash && incomingPath.last != '/')
      incomingPath + '/'
    else
      incomingPath
  }
}
