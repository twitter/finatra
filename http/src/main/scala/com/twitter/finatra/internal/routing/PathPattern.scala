package com.twitter.finatra.internal.routing

import com.twitter.finatra.conversions.pattern._
import java.util.regex.Matcher
import scala.collection.immutable
import scala.util.matching.Regex

object PathPattern {
  /*
   * The regex matches and captures route param names.  Since a ':' character can be used in normal regular expressions
   * for non-capturing groups (e.g., (?:...), we use a negative lookbehind to make sure we don't match a '?:'.
   */
  private val CaptureGroupNamesRegex = """(?<!\?):(\w+)""".r

  /*
   * The regex matches and captures wildcard route params (e.g., :*).  Since a ':' character can be used in normal
   * regular expressions for non-capturing groups (e.g., (?:...), we use a negative lookbehind to make sure we don't
   * match a '?:'.
   */
  private val CaptureGroupAsteriskRegex = """(?<!\?):(\*)$""".r

  /*
   * The regex for asserting valid uri patterns is constructed to match invalid uri patterns and it's negated in the
   * assert.  The regex is broken into two clauses joined by an OR '|'.
   *
   * Part 1: .*?\((?!\?:).*?
   * Matches uris that have a '(' that is not followed by a literal '?:'.  The regex uses negative lookahead to
   * accomplish this.  Since the '(' can occur anywhere or in multiple in the string, we add leading and trailing
   * '.*?' for non-greedy matching of other characters.
   *
   * Part 2: [\(]+
   * Matches uris that have one or more '(' characters.
   *
   * By ORing both parts, we cover all invalid uris.
   */
  private val ValidPathRegex = """.*?\((?!\?:).*?|[\(]+""".r

  private val SomeEmptyMap = Some(Map[String, String]())

  def apply(uriPattern: String): PathPattern = {
    assert(
      !ValidPathRegex.matches(uriPattern),
      "Capture groups are not directly supported. Please use :name syntax instead")

    val regexStr = uriPattern.
      replaceAll( """:\*$""", """(.*)"""). // The special token :* captures everything after the prefix string
      replaceAll( """/:\w+""", """/([^/]+)""") // Replace "colon word (e.g. :id) with a capture group that stops at the next forward slash

    PathPattern(
      regex = new Regex(regexStr),
      captureNames = captureGroupNames(uriPattern))
  }

  /* Private */

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
