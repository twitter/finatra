package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.json._
import java.io.ByteArrayOutputStream
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

class JsonConversionTest extends WordSpec with ShouldMatchers {

  val car = Car("ford", "explorer", 2000)
  val expectedJson = """{"make":"ford","model":"explorer","year":2000}"""

  "json" should {
    "output stream" in {
      val os = new ByteArrayOutputStream()
      car.toJson(os)
      os.toString("UTF-8") should equal(expectedJson)
    }

    "pretty" in {
      car.toPrettyJson should equal(
        """{
          |  "make" : "ford",
          |  "model" : "explorer",
          |  "year" : 2000
          |}""".stripMargin)
    }
  }
}

case class Car(
  make: String,
  model: String,
  year: Int)
