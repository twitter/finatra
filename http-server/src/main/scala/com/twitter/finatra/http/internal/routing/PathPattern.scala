package com.twitter.finatra.http.internal.routing

import com.twitter.finatra.http.exceptions.RouteParamExtractionException
import io.netty.handler.codec.http.QueryStringDecoder
import java.util.regex.Matcher
import scala.collection.immutable
import scala.util.matching.Regex

private[http] object PathPattern {

  /* Matches and captures route param names */
  private val NamedRouteParamRegex = """:\w+""".r

  /* Matches and captures wildcard route param */
  private val NamedAsteriskRegex = """:\*$""".r

  def apply(uriPattern: String): PathPattern = {
    PathPattern(regex = regex(uriPattern), captureNames = captureNames(uriPattern))
  }

  /* Private */

  private def regex(uriPattern: String): Regex = {
    val asterisksReplaced =
      NamedAsteriskRegex.replaceAllIn(
        uriPattern,
        """\\E(.*)\\Q"""
      ) // The special token :* captures everything after the prefix string
    val colonNameReplaced = NamedRouteParamRegex.replaceAllIn(
      asterisksReplaced,
      """\\E([^/]+)\\Q"""
    ) // Replace "colon word (e.g. :id) with a capture group that stops at the next forward slash
    val regexStr = """^\Q""" + colonNameReplaced ++ """\E$"""
    new Regex(regexStr)
  }

  private def captureNames(uriPattern: String): Seq[String] = {
    findNames(uriPattern, NamedRouteParamRegex) ++
      findNames(uriPattern, NamedAsteriskRegex)
  }

  /* We drop to remove the leading ':' */
  private def findNames(uriPattern: String, pattern: Regex): Seq[String] = {
    pattern.findAllIn(uriPattern).toSeq map { _.drop(1) }
  }
}

private[http] case class PathPattern(regex: Regex, captureNames: Seq[String] = Seq()) {

  def extract(requestPath: String): Option[Map[String, String]] = {
    val matcher = regex.pattern.matcher(requestPath)
    if (!matcher.matches)
      None
    else
      Some(extractMatches(matcher))
  }

  //Optimized
  private def extractMatches(matcher: Matcher): Map[String, String] = {
    var idx = 0
    val builder = immutable.Map.newBuilder[String, String]

    for (captureName <- captureNames) {
      idx += 1
      builder += captureName -> decodeComponent(matcher.group(idx))
    }
    builder.result()
  }

  private[this] def decodeComponent(component: String): String = {
    try {
      QueryStringDecoder.decodeComponent(component)
    } catch {
      case e: IllegalArgumentException =>
        throw new RouteParamExtractionException(e)
    }
  }
}
