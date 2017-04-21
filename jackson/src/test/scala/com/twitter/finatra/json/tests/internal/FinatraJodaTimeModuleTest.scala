package com.twitter.finatra.json.tests.internal

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.serde.FinatraSerDeSimpleModule
import com.twitter.inject.Test
import org.joda.time.{DateTime, DateTimeZone}

class FinatraJodaTimeModuleTest extends Test {

  val nowUtc = DateTime.now.withZone(DateTimeZone.UTC)

  test("serialize text") {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    mapper.registerModule(new JodaModule)
    mapper.writeValueAsString(nowUtc) should equal(quote(nowUtc.toString()))
  }

  test("serialize long") {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    mapper.registerModule(new JodaModule)
    mapper.writeValueAsString(nowUtc) should equal(nowUtc.getMillis.toString)
  }

  test("deserialize text") {
    val mapper = new ObjectMapper()
    mapper.registerModule(FinatraSerDeSimpleModule)
    mapper.readValue(quote(nowUtc.toString()), classOf[DateTime]) should equal(nowUtc)
  }

  test("deserialize text with FinatraObjectMapper") {
    val mapper = FinatraObjectMapper.create()
    mapper.registerModule(FinatraSerDeSimpleModule)
    mapper.parse[DateTime](quote(nowUtc.toString())) should equal(nowUtc)
  }

  test("deserialize long") {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    mapper.registerModule(FinatraSerDeSimpleModule)
    mapper.readValue(nowUtc.getMillis.toString(), classOf[DateTime]) should equal(nowUtc)
  }

  def quote(str: String) = {
    '"' + str + '"'
  }
}
