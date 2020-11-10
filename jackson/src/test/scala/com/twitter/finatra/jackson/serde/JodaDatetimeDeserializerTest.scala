package com.twitter.finatra.jackson.serde

import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper => JacksonObjectMapper}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.{ScalaObjectMapper => JacksonScalaObjectMapper}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.Test
import org.joda.time.{DateTime, DateTimeZone}

case class MyClass(dateTime: DateTime)

class JodaDatetimeDeserializerTest extends Test {

  private final val NowUtc: DateTime = DateTime.now.withZone(DateTimeZone.UTC)
  private[this] val myClass1 = MyClass(new DateTime(DateTimeZone.UTC))

  test("serialize long") {
    val mapper = new JacksonObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    mapper.registerModule(new JodaModule)
    mapper.writeValueAsString(NowUtc) should equal(NowUtc.getMillis.toString)
  }

  test("serialize text") {
    val mapper = new JacksonObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    mapper.registerModule(new JodaModule)
    mapper.writeValueAsString(NowUtc) should equal(quote(NowUtc.toString()))
  }

  test("deserialize long") {
    val mapper = new JacksonObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    mapper.registerModule(SerDeSimpleModule)
    mapper.readValue(NowUtc.getMillis.toString, classOf[DateTime]) should equal(NowUtc)
  }

  test("deserialize long with ScalaObjectMapper") {
    val mapper = ScalaObjectMapper()
    val long = 1489719177279L
    val expected: DateTime = new DateTime(long).withZone(DateTimeZone.UTC)
    val parsed = mapper.parse[DateTime](long.toString)
    parsed should equal(expected)
  }

  test("deserialize long as String with ScalaObjectMapper") {
    val mapper = ScalaObjectMapper()
    val longStr = "1489719177279"
    val expected: DateTime = new DateTime(longStr.toLong).withZone(DateTimeZone.UTC)
    val parsed = mapper.parse[DateTime](longStr)
    parsed should equal(expected)
  }

  test("deserialize text") {
    val mapper = new JacksonObjectMapper with JacksonScalaObjectMapper
    mapper.registerModule(new JodaModule)
    mapper.registerModule(SerDeSimpleModule)
    mapper.readValue[DateTime](quote(NowUtc.toString)) should equal(NowUtc)
  }

  test("deserialize text with ScalaObjectMapper with SerDeSimpleModule") {
    val mapper = ScalaObjectMapper()
    mapper.parse[DateTime](quote(NowUtc.toString)) should equal(NowUtc)
  }

  test("roundtrip text") {
    val mapper = new JacksonObjectMapper with JacksonScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JodaModule)
    val json = mapper.writeValueAsString(myClass1)
    val deserializedC1 = mapper.readValue[MyClass](json)
    deserializedC1 should equal(myClass1)
  }

  test("roundtrip text with ScalaObjectMapper with SerDeSimpleModule") {
    val mapper = ScalaObjectMapper()
    val json = mapper.writeValueAsString(myClass1)
    val deserializedC1 = mapper.parse[MyClass](json)
    deserializedC1 should equal(myClass1)
  }

  test("roundtrip text with ScalaObjectMapper") {
    val mapper = ScalaObjectMapper()
    val json = mapper.writeValueAsString(myClass1)
    val deserializedC1 = mapper.parse[MyClass](json)
    deserializedC1 should equal(myClass1)
  }

  private[this] def quote(str: String): String = '"' + str + '"'
}
