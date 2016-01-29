package com.twitter.finatra.json.utils

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.json.FinatraObjectMapper
import org.apache.commons.lang.StringUtils

object JsonDiffResult {

  def create(
    mapper: FinatraObjectMapper,
    expected: JsonNode,
    received: JsonNode): JsonDiffResult = {

    JsonDiffResult(
      expected = expected,
      expectedPrettyString = mapper.writePrettyString(expected),
      received = received,
      receivedPrettyString = mapper.writePrettyString(received))
  }
}

case class JsonDiffResult(
  expected: JsonNode,
  expectedPrettyString: String,
  received: JsonNode,
  receivedPrettyString: String) {

  lazy val toMessage: String = {
    val expectedJsonSorted = JsonDiffUtil.sortedString(expected)
    val receivedJsonSorted = JsonDiffUtil.sortedString(received)

    val expectedHeader = "Expected: "
    val diffStartIdx = StringUtils.indexOfDifference(receivedJsonSorted, expectedJsonSorted)

    val message = new StringBuilder
    message.append(" " * (expectedHeader.length + diffStartIdx) + "*\n")
    message.append(s"Received: $receivedJsonSorted\n")
    message.append(expectedHeader + expectedJsonSorted)
    message.toString()
  }
}
