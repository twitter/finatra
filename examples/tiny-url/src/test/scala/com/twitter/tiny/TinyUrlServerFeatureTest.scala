package com.twitter.tiny

import com.google.inject.testing.fieldbinder.Bind
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.Mockito
import com.twitter.inject.server.FeatureTest
import com.twitter.tiny.domain.http.PostUrlResponse
import com.twitter.tiny.services._
import org.mockito.Matchers.anyObject
import redis.clients.jedis.{Jedis => JedisClient}
import scala.util.Random

class TinyUrlServerFeatureTest
  extends FeatureTest
  with Mockito {

  override val server = new EmbeddedHttpServer(new TinyUrlServer)

  @Bind
  val mockJedisClient = smartMock[JedisClient]

  "Server" should {
    "return shortened url" in {
      mockJedisClient.get(anyObject[String]()) returns null
      mockJedisClient.set(
        anyObject[String](),
        anyObject[String]()) returns "OK"

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

    "resolve shortened url" in {
      mockJedisClient.get(anyObject[String]()) returns null
      mockJedisClient.set(
        anyObject[String](),
        anyObject[String]()) returns "OK"

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
        anyObject[String]()) returns "http://www.google.com"

      server.httpGet(
        path = response.tinyUrl.substring(response.tinyUrl.lastIndexOf("/")),
        andExpect = MovedPermanently)
    }

    "return BadRequest for garbage url" in {
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

    "return NotFound for unknown tiny url" in {
      val id =
        java.lang.Long.toString(
          Counter.InitialValue + new Random(Counter.InitialValue).nextLong().abs,
          EncodingRadix)

      mockJedisClient.get(anyObject[String]()) returns null

      server.httpGet(
        path = s"/$id",
        andExpect = NotFound)
    }
  }
}
