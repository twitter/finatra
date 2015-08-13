package com.twitter.finatra.http.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.test.EmbeddedTwitterServer
import com.twitter.inject.server.AsyncStreamUtils
import com.twitter.io.Buf
import com.twitter.logging.Logger
import com.twitter.util.{Await, FuturePool}
import java.net.URLEncoder
import java.util.logging.Level
import org.apache.commons.io.IOUtils

trait HttpTest
  extends com.twitter.inject.Test
  with HttpMockResponses {

  protected val testClientAppId = 12345L
  protected val mapper = FinatraObjectMapper.create()
  protected val pool = FuturePool.unboundedPool

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
    if (objNode.has("id")) {
      objNode.put("id", NormalizedId)
    }
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

  def resolverMap(resolverMap: (String, String)*): String = {
    if (resolverMap.isEmpty)
      ""
    else
      "-com.twitter.server.resolverMap=" + {
        resolverMap map { case (k, v) =>
          k + "=" + v
        } mkString ","
      }
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

  def deserializeRequest(name: String) = {
    val requestBytes = IOUtils.toByteArray(getClass.getResourceAsStream(name))
    Request.decodeBytes(requestBytes)
  }

  def writeJsonArray(request: Request, seq: Seq[Any], delayMs: Long): Unit = {
    request.writeAndWait("[")
    request.writeAndWait(seq.head.toJson)
    for (elem <- seq.tail) {
      request.writeAndWait("," + elem.toJson)
      Thread.sleep(delayMs)
    }
    request.writeAndWait("]")
    request.closeAndWait()
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

  /* Request Implicit Utils */

  implicit class RichRequest(request: Request) {
    def writeAndWait(str: String) {
      println("Write:\t" + str)
      Await.result(
        request.writer.write(Buf.Utf8(str)))
    }

    def closeAndWait() {
      Await.result(
        request.close())
    }
  }

  implicit class RichResponse(response: Response) {
    def asyncStrings = {
      AsyncStreamUtils.readerToAsyncStream(response.reader) map { case Buf.Utf8(str) =>
        str
      }
    }

    def printAsyncStrings() = {
      Await.result(
        response.asyncStrings map { "Read:\t" + _ } foreach println)
    }
  }

}
