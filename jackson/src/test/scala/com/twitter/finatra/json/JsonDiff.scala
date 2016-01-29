package com.twitter.finatra.json

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.json.utils.{JsonDiffResult, JsonDiffUtil}
import com.twitter.inject.Logging
import org.scalatest.exceptions.TestFailedException

object JsonDiff extends Logging {

  def jsonDiff[T](
    receivedJson: Any,
    expectedJson: Any,
    normalizer: JsonNode => JsonNode = null,
    verbose: Boolean = true) {
    assertJsonNodesSame(
      jsonDiffResultOpt =
        JsonDiffUtil.jsonDiff[T](
          receivedJson,
          expectedJson,
          normalizer),
      verbose = verbose)
  }

  // extract json nodes, sort them, and assert that they are each the same
  def jsonSetsDiff(receivedJsonSet: Set[Any], expectedJsonSet: Set[Any]) = {
    val receivedJsonNodes = JsonDiffUtil.toSortedJsonNodes(receivedJsonSet)
    val expectedJsonNodes = JsonDiffUtil.toSortedJsonNodes(receivedJsonSet)
    val compares = receivedJsonNodes.zip(expectedJsonNodes)
    compares.foreach { case (received, expected) =>
      assertJsonNodesSame(
        JsonDiffUtil.jsonDiff(received, expected),
        verbose = true)
    }
  }

  /* Private */

  private def assertJsonNodesSame(
    jsonDiffResultOpt: Option[JsonDiffResult],
    verbose: Boolean) = {

    jsonDiffResultOpt map { result =>
      println("JSON DIFF FAILED!")
      if (verbose) {
        println(s"Received:\n ${result.receivedPrettyString}\n")
        println(s"Expected:\n ${result.expectedPrettyString}\n")
      }

      println(result.toMessage)
      throw new TestFailedException(s"Json diff failed: \n${result.toMessage}", 1)
    }
  }
}
