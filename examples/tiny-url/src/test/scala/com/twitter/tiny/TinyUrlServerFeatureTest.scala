package com.twitter.tiny

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.Mockito
import com.twitter.inject.server.FeatureTest
import com.twitter.tiny.domain.http.PostUrlResponse
import com.twitter.tiny.services._
import redis.clients.jedis.{Jedis => JedisClient}
import scala.util.Random

class TinyUrlServerFeatureTest
  extends FeatureTest
  with Mockito {

  val mockJedisClient = smartMock[JedisClient]
  override val server =
    new EmbeddedHttpServer(new TinyUrlServer)
        .bind[JedisClient](mockJedisClient)

  test("Server#return shortened url") {
    mockJedisClient.get(any[String]) returns null
    mockJedisClient.set(
      any[String],
      any[String]) returns "OK"

    val port = server.httpExternalPort
    val path =
      java.lang.Long.toString(Counter.InitialValue + 1, EncodingRadix)

    server.httpPost(
      path = "/url",
      postBody =
        """
          {
            "url" : "http://www.google.com"
          }
        """,
      andExpect = Created,
      withJsonBody =
        s"""
          {
            "tiny_url" : "http://127.0.0.1:$port/$path"
          }
        """)
  }

  test("Server#resolve shortened url") {
    mockJedisClient.get(any[String]) returns null
    mockJedisClient.set(
      any[String],
      any[String]) returns "OK"

    val response = server.httpPostJson[PostUrlResponse](
      path = "/url",
      postBody =
        """
          {
            "url" : "http://www.google.com"
          }
        """,
      andExpect = Created)

    mockJedisClient.get(
      any[String]) returns "http://www.google.com"

    server.httpGet(
      path = response.tinyUrl.substring(response.tinyUrl.lastIndexOf("/")),
      andExpect = MovedPermanently)
  }

  test("Server# return BadRequest for garbage url") {
    server.httpPost(
      path = "/url",
      postBody =
        """
          {
            "url" : "foo://-oomw384$*garbageoogle.^com"
          }
        """,
      andExpect = BadRequest)
  }

  test("Server#return NotFound for unknown tiny url") {
    val id =
      java.lang.Long.toString(
        Counter.InitialValue + math.abs(new Random(Counter.InitialValue).nextLong()),
        EncodingRadix)

    mockJedisClient.get(any[String]) returns null

    server.httpGet(
      path = s"/$id",
      andExpect = NotFound)
  }
}
