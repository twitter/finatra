package com.twitter.finatra.json.internal

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

class FinatraJodaTimeModuleTest extends WordSpec with ShouldMatchers {

  val nowUtc = DateTime.now.withZone(DateTimeZone.UTC)

  "serialize text" in {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    mapper.registerModule(FinatraSerDeSimpleModule)
    mapper.writeValueAsString(nowUtc) should equal(quote(nowUtc.toString()))
  }

  "serialize long" in {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    mapper.registerModule(FinatraSerDeSimpleModule)
    mapper.writeValueAsString(nowUtc) should equal(nowUtc.getMillis.toString)
  }

  "deserialize text" in {
    val mapper = new ObjectMapper()
    mapper.registerModule(FinatraSerDeSimpleModule)
    mapper.readValue(quote(nowUtc.toString()), classOf[DateTime]) should equal(nowUtc)
  }

  "deserialize long" in {
    val mapper = new ObjectMapper()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
    mapper.registerModule(FinatraSerDeSimpleModule)
    mapper.readValue(nowUtc.getMillis.toString(), classOf[DateTime]) should equal(nowUtc)
  }

  def quote(str: String) = {
    '"' + str + '"'
  }
}
