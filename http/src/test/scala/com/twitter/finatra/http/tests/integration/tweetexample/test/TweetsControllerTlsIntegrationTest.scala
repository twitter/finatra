package com.twitter.finatra.http.tests.integration.tweetexample.test

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.ChannelClosedException
import com.twitter.finagle.http.{Request, Status}
import com.twitter.finatra.http.tests.integration.tweetexample.main.TweetsEndpointServer
import com.twitter.finatra.http.{EmbeddedHttpServer, JsonAwareEmbeddedHttpClient}
import com.twitter.inject.server.FeatureTest
import java.io.{File, FileWriter}
import scala.collection.mutable
import scala.io.Source

class TweetsControllerTlsIntegrationTest extends FeatureTest {

  private val onWriteLog: mutable.ArrayBuffer[String] = new mutable.ArrayBuffer[String]()

  private def loadResourceFile(filename: String): String = {
    val fileSource = Source.fromInputStream(getClass.getResourceAsStream(filename))
    try fileSource.mkString
    finally fileSource.close()
  }

  private def createCryptoMaterial(filePrefix: String, filePath: String): File = {
    val tempFile = File.createTempFile("temp", filePrefix)
    tempFile.deleteOnExit()
    val writer = new FileWriter(tempFile)
    writer.write(loadResourceFile(filePath))
    writer.close()
    tempFile
  }

  private val cert: File = createCryptoMaterial(".crt", "/certs/test-rsa.crt")
  private val chain: File = createCryptoMaterial(".chain", "/certs/test-rsa-chain.crt")
  private val keyFile: File = createCryptoMaterial(".key", "/keys/test-pkcs8.key")

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    new TweetsEndpointServer,
    defaultRequestHeaders = Map("X-UserId" -> "123"),
    // Set client flags to also start on HTTPS port
    flags = Map(
      "http.port" -> "",
      "https.port" -> ":0",
      "cert.path" -> cert.getAbsolutePath,
      "key.path" -> keyFile.getAbsolutePath,
      "chain.path" -> chain.getAbsolutePath
    ),
    disableTestLogging = true
  ).bind[mutable.ArrayBuffer[String]].toInstance(onWriteLog)

  override def beforeEach(): Unit = {
    super.beforeEach()
    onWriteLog.clear()
  }

  private val request: Request = Request("/tweets/1")

  test("get tweet 1 with https") {
    val tweet =
      server.httpGetJson[Map[String, Long]]("/tweets/1", andExpect = Status.Ok, secure = Some(true))

    tweet("idonly") should equal(
      1
    ) //confirm response was transformed by registered TweetMessageBodyWriter
  }

  test("get tweet 1 with http should fail") {
    val httpClient: JsonAwareEmbeddedHttpClient = {
      val client = new JsonAwareEmbeddedHttpClient(
        "httpClient",
        server.httpsExternalPort(),
        tls = false,
        sessionAcquisitionTimeout = 1.second,
        streamResponses = server.streamResponse,
        defaultHeaders = () => server.defaultRequestHeaders,
        server.mapper,
        disableLogging = true
      )
      server.closeOnExit(client)
      client
    }
    assertThrows[ChannelClosedException](httpClient(request))
  }
}
