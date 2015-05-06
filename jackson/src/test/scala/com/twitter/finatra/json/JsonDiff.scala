package com.twitter.finatra.json

import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapperCopier, SerializationFeature}
import com.twitter.inject.Logging
import com.twitter.util.NonFatal
import org.apache.commons.lang.StringUtils
import org.scalatest.exceptions.TestFailedException

object JsonDiff extends Logging {

  private val finatraMapper = FinatraObjectMapper.create()

  def jsonDiff[T](
    recvJson: Any,
    expectedJson: Any,
    normalizer: JsonNode => JsonNode = null,
    verbose: Boolean = true) {

    val recvJsonStr = jsonString(recvJson)
    val expectedJsonStr = jsonString(expectedJson)

    val recvJsonNode = {
      val jsonNode = tryJsonNodeParse(recvJsonStr)

      if (normalizer != null)
        normalizer(jsonNode)
      else
        jsonNode
    }

    val expectedJsonNode = tryJsonNodeParse(expectedJsonStr)
    assertJsonNodesSame(recvJsonNode, expectedJsonNode, verbose)
  }

  private def tryJsonNodeParse(expectedJsonStr: String): JsonNode = {
    try {
      finatraMapper.parse[JsonNode](expectedJsonStr)
    } catch {
      case NonFatal(e) =>
        println(e.getMessage)
        new TextNode(expectedJsonStr)
    }
  }

  private def assertJsonNodesSame(recv: JsonNode, expected: JsonNode, verbose: Boolean) = {
    if (recv != expected) {
      val recvJsonNormalized = generateSortedAlpha(recv)
      val expectedJsonNormalized = generateSortedAlpha(expected)

      val expectedHeader = "Expected: "
      val diffStartIdx = StringUtils.indexOfDifference(recvJsonNormalized, expectedJsonNormalized)

      println("JSON DIFF FAILED!")
      if (verbose) {
        println("Received:\n" + finatraMapper.writePrettyString(recv) + "\n")
        println("Expected:\n" + finatraMapper.writePrettyString(expected) + "\n")
      }

      println(" " * (expectedHeader.length + diffStartIdx) + "*")
      println("Received: " + recvJsonNormalized)
      println(expectedHeader + expectedJsonNormalized)
      throw new TestFailedException("Json diff failed", 1)
    }
  }

  private def jsonString(recvJson: Any): String = recvJson match {
    case str: String => str
    case _ => finatraMapper.writeValueAsString(recvJson)
  }

  /** Normalizer function is optional. We default to null to make the calling API cleaner */
  def assertJsonStr[T: Manifest](recv: String, expectedJson: String, normalizer: T => T = null): T = {
    val receivedObj = finatraMapper.parse[T](recv)
    val expectedObj = finatraMapper.parse[T](expectedJson)

    if (normalizer != null)
      jsonDiff(normalizer(receivedObj), normalizer(expectedObj))
    else
      jsonDiff(receivedObj, expectedObj)

    receivedObj
  }

  private def toSortedJsonNodes(s: Set[Any]) = {
    //parse json and back out to normalized strings for sorting
    val normalizedStrings = for {
      member <- s
    } yield generateSortedAlpha(finatraMapper.parse[JsonNode](jsonString(member)))

    //sort normalized strings and convert back to json nodes
    normalizedStrings.toList.sorted.map { str =>
      finatraMapper.parse[JsonNode](str)
    }
  }

  //extract json nodes, sort them, and assert that they are each the same
  def jsonSetsDiff(recvJsonSet: Set[Any], expectedJsonSet: Set[Any]) = {
    val recvJsonNodes = toSortedJsonNodes(recvJsonSet)
    val expJsonNodes = toSortedJsonNodes(recvJsonSet)
    val compares = recvJsonNodes.zip(expJsonNodes)
    compares.foreach { case (received, expected) =>
      assertJsonNodesSame(received, expected, verbose = true)
    }
  }

  /* Used in tests to provide stable sorted output for assertions */
  def generateSortedAlpha(jsonNode: JsonNode): String = {
    if (jsonNode.isTextual) {
      jsonNode.textValue()
    } else {
      val node = sortingObjectMapper.treeToValue(jsonNode, classOf[Object])
      sortingObjectMapper.writeValueAsString(node)
    }
  }

  private lazy val sortingObjectMapper = {
    val newMapper = ObjectMapperCopier.copy(finatraMapper.objectMapper)
    newMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    newMapper
  }
}
