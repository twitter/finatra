package com.twitter.finatra.tests.json

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.JsonDiff._
import com.twitter.inject.Logging
import org.scalatest.{Matchers, WordSpec}

class JsonDiffTest
  extends WordSpec
  with Matchers
  with Logging {

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

  "generate sorted" in {
    val mapper = FinatraObjectMapper.create()
    val before = mapper.parse[JsonNode]("""{"a":1,"c":3,"b":2}""")
    val expected = """{"a":1,"b":2,"c":3}"""
    generateSortedAlpha(before) should equal(expected)
  }
}
