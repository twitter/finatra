package com.twitter.finatra.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.logging.Logger
import java.net.URLEncoder
import java.util.logging.Level

trait HttpTest
  extends com.twitter.inject.Test
  with HttpMockResponses {

  protected val testClientAppId = 12345L
  protected val mapper = FinatraObjectMapper.create()

  override protected def beforeAll() {
    super.beforeAll()
    configFinagleLogging()
  }

  def configFinagleLogging() {
    val finagleLog = Logger("finagle")
    finagleLog.setLevel(Level.WARNING)
  }

  val NormalizedId = "0"

  def idNormalizer(jsonNode: JsonNode): JsonNode = {
    val objNode = jsonNode.asInstanceOf[ObjectNode]
    objNode.put("id", NormalizedId)
    objNode
  }

  @deprecated("Use server.assertHealthy()", "")
  def assertHealth(server: EmbeddedTwitterServer) = {
    server.assertHealthy()
    server
  }

  @deprecated("Use server.assertHealthy()", "")
  def assertHealth(server: EmbeddedHttpServer, healthy: Boolean = true) = {
    server.assertHealthy(healthy)
    server
  }

  def urlEncode(str: String) = {
    URLEncoder.encode(str, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }

  /* JSON Implicit Utils */

  implicit class RichAny(any: Any) {
    def toJson = {
      mapper.writeValueAsString(any)
    }

    def toPrettyJson = {
      mapper.writePrettyString(any)
    }

    def toPrettyJsonStdout(): Unit = {
      println(
        mapper.writePrettyString(any))
    }

    def parseJson[T: Manifest]: T = {
      any match {
        case str: String =>
          mapper.parse[JsonNode](str).parseJson[T]
        case _ =>
          mapper.convert[T](any)
      }
    }
  }

}
