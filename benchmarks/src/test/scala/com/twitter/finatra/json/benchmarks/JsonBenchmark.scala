package com.twitter.finatra.json.benchmarks

import com.fasterxml.jackson.module.scala.JacksonModule
import com.twitter.finatra.StdBenchAnnotations
import com.twitter.finatra.benchmarks.domain.{TestDemographic, TestFormat}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import java.io.ByteArrayInputStream
import org.joda.time.DateTime
import org.openjdk.jmh.annotations._

/**
 * ./sbt 'project benchmarks' 'jmh:run JsonBenchmark'
 */
@State(Scope.Thread)
class JsonBenchmark extends StdBenchAnnotations {

  private[this] val json =
    """{
        "request_id": "00000000-1111-2222-3333-444444444444",
        "type": "impression",
        "params": {
          "priority": "normal",
          "start_time": "2013-01-01T00:02:00.000Z",
          "end_time": "2013-01-01T00:03:00.000Z",
          "results": {
            "demographics": ["age", "gender"],
            "country_codes": ["US"],
            "group_by": ["group"]
          }
        },
        "group_ids":["grp-1"]
      }"""

  private[this] val bytes = json.getBytes("UTF-8")

  val mapperWithCaseClassDeserializer: FinatraObjectMapper = {
    val mapperModule = new FinatraJacksonModule()
    val scalaObjectMapper = mapperModule.provideScalaObjectMapper(injector = null)
    new FinatraObjectMapper(scalaObjectMapper)
  }

  val mapperWithoutCaseClassDeserializer: FinatraObjectMapper = {
    val mapperModule: FinatraJacksonModule = new FinatraJacksonModule() {
      // omit FinatraCaseClassModule
      override def finatraCaseClassModule: Option[JacksonModule] = None
    }
    val scalaObjectMapper = mapperModule.provideScalaObjectMapper(injector = null)
    new FinatraObjectMapper(scalaObjectMapper)
  }

  @Benchmark
  def withCaseClassDeserializer(): TestTask = {
    val is = new ByteArrayInputStream(bytes)
    mapperWithCaseClassDeserializer.parse[TestTask](is)
  }

  @Benchmark
  def withoutCaseClassDeserializer(): TestTask = {
    val is = new ByteArrayInputStream(bytes)
    mapperWithoutCaseClassDeserializer.parse[TestTask](is)
  }
}

case class TestTask(request_id: String, group_ids: Seq[String], params: TestTaskParams)

case class TestTaskParams(
  results: TaskTaskResults,
  start_time: DateTime,
  end_time: DateTime,
  priority: String
)

case class TaskTaskResults(
  country_codes: Seq[String],
  group_by: Seq[String],
  format: Option[TestFormat],
  demographics: Seq[TestDemographic]
)
