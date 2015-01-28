package com.twitter.finatra.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.twitter.finagle.http.Status
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.logging.Logger
import java.net.URLEncoder
import java.util.logging.Level

trait HttpTest extends Test {

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

  def urlEncode(str: String) = {
    URLEncoder.encode(str, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }

  val NormalizedId = "0"

  def idNormalizer(jsonNode: JsonNode): JsonNode = {
    val objNode = jsonNode.asInstanceOf[ObjectNode]
    objNode.put("id", NormalizedId)
    objNode
  }

  @deprecated("use server.assertHealthy()", "1/21/15")
  def assertHealth(server: EmbeddedTwitterServer, healthy: Boolean = true) = {
    server.start()
    assert(server.twitterServer.httpAdminPort != 0)
    val expectedBody = if(healthy) "OK\n" else ""

    server.httpGet(
      "/health",
      routeToAdminServer = true,
      andExpect = Status.Ok,
      withBody = expectedBody)

    server
  }
}