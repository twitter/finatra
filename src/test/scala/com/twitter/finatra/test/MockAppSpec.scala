package com.twitter.finatra.test

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.twitter.finatra.Controller
import scala.collection.JavaConverters._
import org.jboss.netty.handler.codec.http.HttpMethod
import com.twitter.finagle.http.Request

class MockAppSpec extends FlatSpec with ShouldMatchers {
  val server = new MockApp(new Controller)

  "#toByteArray" should "directly convert String to Array[Byte]" in {
    val value = "hello world"
    server.toByteArray(value).get should be(value.getBytes)
  }

  it should "also directly convert Array[Byte] to Array[Byte]" in {
    val value = "hello world".getBytes
    server.toByteArray(value).get should be(value)
  }

  it should "convert Map[String, String] to url-encoded form data" in {
    val value = Map("hello" -> "world")
    server.toByteArray(value).get should be("hello=world".getBytes)
  }

  it should "convert util.Map[String, String] to url-encoded form data" in {
    val value = Map("hello" -> "world").asJava
    server.toByteArray(value).get should be("hello=world".getBytes)
  }

  it should "convert null to None" in {
    server.toByteArray(null) should be(None)
  }

  it should "attempt to convert other objects to a json equivalent" in {
    val sample = Sample("matt", "matt@does-not-exist.com")
    server.toByteArray(sample).get should be( """{"name":"matt","email":"matt@does-not-exist.com"}""".getBytes)
  }

  "#buildRequest" should "apply body if present" in {
    val sample = Sample("matt", "matt@does-not-exist.com")

    // When
    val request: Request = server.buildRequest(HttpMethod.POST, "/", body = sample)

    // Then
    request.contentString should be(MockApp.mapper.writeValueAsString(sample))
  }

  it should "not allow both params AND a non-null body in the same request" in {
    val sample = Sample("matt", "matt@does-not-exist.com")

    evaluating {
      server.buildRequest(HttpMethod.POST, "/", params = Map("hello" -> "world"), body = sample)
    } should produce[RuntimeException]
  }

  case class Sample(name: String, email: String)
}