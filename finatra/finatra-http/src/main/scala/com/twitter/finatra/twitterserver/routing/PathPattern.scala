package com.twitter.finatra.twitterserver.routing

import java.util.regex.Matcher
import scala.collection.immutable
import scala.util.matching.Regex

object PathPattern {
  private val CaptureGroupNamesRegex = """:(\w+)""".r
  private val CaptureGroupAsteriskRegex = """:(\*)$""".r
  private val SomeEmptyMap = Some(Map[String, String]())

  def apply(uriPattern: String): PathPattern = {
    val regexStr = uriPattern.
      replaceAll( """:\*$""", """(.*)"""). // The special token :* captures everything after the prefix string
      replaceAll( """/:\w+""", """/([^/]+)""") // Replace "colon word (e.g. :id) with a capture group that stops at the next forward slash

    PathPattern(
      regex = new Regex(regexStr),
      captureNames = captureGroupNames(uriPattern))
  }

  private def captureGroupNames(uriPattern: String): Seq[String] = {
    findNames(uriPattern, CaptureGroupNamesRegex) ++
      findNames(uriPattern, CaptureGroupAsteriskRegex)
  }

  /* We drop(1) to remove the leading ':' */
  private def findNames(uriPattern: String, pattern: Regex): Seq[String] = {
    pattern.findAllIn(uriPattern).toSeq map {_.drop(1)}
  }
}

case class PathPattern(
  regex: Regex,
  captureNames: Seq[String] = Seq()) {

  private val emptyCaptureNames = captureNames.isEmpty

  def extract(requestPath: String): Option[Map[String, String]] = {
    val matcher = regex.pattern.matcher(requestPath)
    if (!matcher.matches)
      None
    else if (emptyCaptureNames)
      PathPattern.SomeEmptyMap
    else
      Some(extractMatches(matcher))
  }

  //Optimized
  private def extractMatches(matcher: Matcher): Map[String, String] = {
    var idx = 0
    val builder = immutable.Map.newBuilder[String, String]

    for (captureName <- captureNames) {
      idx += 1
      builder += captureName -> matcher.group(idx)
    }
    builder.result()
  }
}
