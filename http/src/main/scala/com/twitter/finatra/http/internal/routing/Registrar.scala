package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.Filter
import com.twitter.inject.internal.LibraryRegistry

/** Performs registration of HTTP domain entities in a LibraryRegistry */
private[http] class Registrar(registry: LibraryRegistry) {

  /**
   * Registers an HTTP [[Route]] in the given [[LibraryRegistry]]. HTTP routes are unique
   * per HTTP verb + URI. Thus information is keyed under each Route by HTTP verb.
   *
   * {{{
   *         "/foo": {
   *           "post": {
   *             "constant": "true",
   *             "method": "POST",
   *             "admin": "false",
   *             "path": "/foo",
   *             "callback": {
   *               "request": "com.twitter.finagle.http.Request",
   *               "response": "..."
   *             },
   *             "class": "com.twitter.finatra.example.Controller"
   *           },
   *           "get": {
   *             "constant": "true",
   *             "method": "GET",
   *             "admin": "false",
   *             "path": "/foo",
   *             "callback": {
   *               "response": "...",
   *               "request": "com.twitter.finagle.http.Request"
   *             },
   *             "class": "com.twitter.finatra.example.Controller"
   *           },
   *           "options": {
   *             "constant": "true",
   *             "method": "OPTIONS",
   *             "admin": "false",
   *             "path": "/foo",
   *             "callback": {
   *               "response": "...",
   *               "request": "com.twitter.finagle.http.Request"
   *             },
   *             "class": "com.twitter.finatra.example.Controller"
   *           }
   *         }
   * }}}
   *
   * @param route the [[com.twitter.finatra.http.internal.routing.Route]] to register.
   */
  def register(route: Route): Unit = {
    val name = if (route.name.isEmpty) route.path else route.name

    registry.put(Seq(name, route.method.name, "class"), route.clazz.getName)
    registry.put(Seq(name, route.method.name, "path"), route.path)
    registry.put(Seq(name, route.method.name, "constant"), route.constantRoute.toString)

    registry.put(Seq(name, route.method.name, "callback", "request"), route.requestClass.toString)
    registry.put(Seq(name, route.method.name, "callback", "response"), route.responseClass.toString)
    registry.put(Seq(name, route.method.name, "method"), route.method.name)

    if (route.captureNames.nonEmpty) {
      registry.put(Seq(name, route.method.name, "capture_names"), route.captureNames.mkString(","))
    }

    if (route.routeFilter ne Filter.identity) {
      registry.put(Seq(name, route.method.name, "filters"), route.routeFilter.toString)
    }

    route.index match {
      case Some(index) =>
        val adminIndexKey = Seq(name, route.method.name, "admin", "index")
        registry.put(
          key = adminIndexKey :+ "alias",
          value = index.alias
        )
        registry.put(
          key =  adminIndexKey :+ "group",
          value = index.group
        )
        registry.put(
          key = adminIndexKey :+ "method",
          value = index.method.name
        )
        if (index.path.isDefined) {
          registry.put(
            key =  adminIndexKey :+ "path",
            value = index.path.get
          )
        }
      case _ =>
        registry.put(Seq(name, route.method.name, "admin"), route.admin.toString)
    }
  }
}
