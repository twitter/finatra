package com.twitter.finatra.json.benchmarks

import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.benchmarks.domain.{TestDemographic, TestFormat}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.serde.FinatraSerDeSimpleModule
import com.twitter.finatra.json.modules.FinatraJacksonModule
import java.io.ByteArrayInputStream
import org.joda.time.DateTime
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class JsonBenchmark {

  val json = """
      {
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

  val bytes = json.getBytes("UTF-8")

  val finatraObjectMapper = {
    val mapperModule = new FinatraJacksonModule()
    val scalaObjectMapper = mapperModule.provideScalaObjectMapper(injector = null)
    new FinatraObjectMapper(scalaObjectMapper)
  }

  val jacksonScalaModuleObjectMapper = {
    val mapperModule = new FinatraJacksonModule() {

      // omit FinatraCaseClassModule
      override def defaultJacksonModules = Seq(
        new JodaModule,
        DefaultScalaModule,
        FinatraSerDeSimpleModule)
    }
    val scalaObjectMapper = mapperModule.provideScalaObjectMapper(injector = null)
    new FinatraObjectMapper(scalaObjectMapper)
  }


  @Benchmark
  def finatraCustomCaseClassDeserializer() = {
    val is = new ByteArrayInputStream(bytes)
    finatraObjectMapper.parse[TestTask](is)
  }

  @Benchmark
  def jacksonScalaModuleCaseClassDeserializer() = {
    val is = new ByteArrayInputStream(bytes)
    jacksonScalaModuleObjectMapper.parse[TestTask](is)
  }
}

case class TestTask(
  request_id: String,
  group_ids: Seq[String],
  params: TestTaskParams)

case class TestTaskParams(
  results: TaskTaskResults,
  start_time: DateTime,
  end_time: DateTime,
  priority: String)

case class TaskTaskResults(
  country_codes: Seq[String],
  group_by: Seq[String],
  format: Option[TestFormat],
  demographics: Seq[TestDemographic])
