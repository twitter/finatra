package com.twitter.finatra.json.tests

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.JsonDiff._
import com.twitter.finatra.json.utils.JsonDiffUtil
import com.twitter.inject.Test
import org.scalatest.exceptions.TestFailedException

class JsonDiffTest extends Test {
  "json diff test" in {
    val a =
      """
      {
        "a": 1,
        "b": 2
      }
      """

    val b =
      """
      {
        "b": 2,
        "a": 1
      }
      """

    jsonDiff(a, b)
  }

  "json diff failure" in {

    val a =
    """
      {
        "a": 1,
        "b": 2
      }
    """

    val b =
    """
      {
        "a": 11,
        "b": 2
      }
    """

    intercept[TestFailedException] {
      jsonDiff(
        receivedJson = a,
        expectedJson = b,
        verbose = false)
    }
  }

  "generate sorted" in {
    val mapper = FinatraObjectMapper.create()
    val before = mapper.parse[JsonNode]("""{"a":1,"c":3,"b":2}""")
    val expected = """{"a":1,"b":2,"c":3}"""
    JsonDiffUtil.sortedString(before) should equal(expected)
  }
}
