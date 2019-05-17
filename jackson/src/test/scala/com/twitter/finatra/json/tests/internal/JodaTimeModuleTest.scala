package com.twitter.finatra.json.tests.internal

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.serde.SerDeSimpleModule
import com.twitter.inject.Test
import org.joda.time.{DateTime, DateTimeZone}

case class MyClass(dateTime: DateTime)

class JodaTimeModuleTest extends Test {

  private final val NowUtc: DateTime = DateTime.now.withZone(DateTimeZone.UTC)
  private[this] val myClass1 = MyClass(new DateTime(DateTimeZone.UTC))

  test("serialize long") {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    mapper.registerModule(new JodaModule)
    mapper.writeValueAsString(NowUtc) should equal(NowUtc.getMillis.toString)
  }

  test("serialize text") {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    mapper.registerModule(new JodaModule)
    mapper.writeValueAsString(NowUtc) should equal(quote(NowUtc.toString()))
  }

  test("deserialize long") {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    mapper.registerModule(SerDeSimpleModule)
    mapper.readValue(NowUtc.getMillis.toString, classOf[DateTime]) should equal(NowUtc)
  }

  test("deserialize long with FinatraObjectMapper") {
    val mapper = FinatraObjectMapper.create()
    mapper.registerModule(SerDeSimpleModule)

    val long = 1489719177279L
    val expected: DateTime = new DateTime(long).withZone(DateTimeZone.UTC)
    val parsed = mapper.parse[DateTime](long.toString)
    parsed should equal(expected)
  }

  test("deserialize long as String with FinatraObjectMapper") {
    val mapper = FinatraObjectMapper.create()
    mapper.registerModule(SerDeSimpleModule)

    val longStr = "1489719177279"
    val expected: DateTime = new DateTime(longStr.toLong).withZone(DateTimeZone.UTC)
    val parsed = mapper.parse[DateTime](longStr)
    parsed should equal(expected)
  }

  test("deserialize text") {
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModule(new JodaModule)
    mapper.registerModule(SerDeSimpleModule)
    mapper.readValue[DateTime](quote(NowUtc.toString)) should equal(NowUtc)
  }

  test("deserialize text with FinatraObjectMapper with SerDeSimpleModule") {
    val mapper = FinatraObjectMapper.create()
    mapper.registerModule(SerDeSimpleModule)
    mapper.parse[DateTime](quote(NowUtc.toString)) should equal(NowUtc)
  }

  test("roundtrip text") {
    val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JodaModule)

    val json = mapper.writeValueAsString(myClass1)

    val deserializedC1 = mapper.readValue[MyClass](json)
    deserializedC1 should equal(myClass1)
  }

  test("roundtrip text with FinatraObjectMapper with SerDeSimpleModule") {
    val mapper = FinatraObjectMapper.create()
    mapper.registerModule(SerDeSimpleModule)

    val json = mapper.writeValueAsString(myClass1)

    val deserializedC1 = mapper.parse[MyClass](json)
    deserializedC1 should equal(myClass1)
  }

  test("roundtrip text with FinatraObjectMapper") {
    val mapper = FinatraObjectMapper.create()

    val json = mapper.writeValueAsString(myClass1)

    val deserializedC1 = mapper.parse[MyClass](json)
    deserializedC1 should equal(myClass1)
  }

  private[this] def quote(str: String): String = '"' + str + '"'
}
