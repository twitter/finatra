package com.twitter.finatra.json.tests.internal

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.json.internal.serde.SerDeSimpleModule
import com.twitter.inject.Test
import com.twitter.util.{Time, TimeFormat}

case class WithoutJsonFormat(time: Time)

case class WithJsonFormat(@JsonFormat(pattern = "yyyy-MM-dd") time: Time)

case class WithJsonFormatAndTimezone(
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "America/Los_Angeles") time: Time
)

class TwitterTimeStringDeserializerTest extends Test {

  private[this] final val Input1 =
    """
      |{
      |  "time": "2019-06-17T15:45:00.000+0000"
      |}
    """.stripMargin

  private[this] final val Input2 =
    """
      |{
      |  "time": "2019-06-17"
      |}
    """.stripMargin

  private[this] final val Input3 =
    """
      |{
      |  "time": "2019-06-17 16:30:00"
      |}
    """.stripMargin

  private[this] val objectMapper = new ObjectMapper()

  override def beforeAll(): Unit = {
    val modules = Seq(DefaultScalaModule, SerDeSimpleModule)
    modules.foreach(objectMapper.registerModule)
  }

  test("should deserialize date without JsonFormat") {
    val expected = 1560786300000L
    val actual: WithoutJsonFormat = objectMapper.readValue(Input1, classOf[WithoutJsonFormat])
    actual.time.inMillis shouldEqual expected
  }

  test("should deserialize date with JsonFormat") {
    val expected: Time = new TimeFormat("yyyy-MM-dd").parse("2019-06-17")
    val actual: WithJsonFormat = objectMapper.readValue(Input2, classOf[WithJsonFormat])
    actual.time shouldEqual expected
  }

  test("should deserialize date with JsonFormat and timezone") {
    val expected = 1560814200000L
    val actual: WithJsonFormatAndTimezone = objectMapper.readValue(
      Input3, classOf[WithJsonFormatAndTimezone])
    actual.time.inMillis shouldEqual expected
  }

}
