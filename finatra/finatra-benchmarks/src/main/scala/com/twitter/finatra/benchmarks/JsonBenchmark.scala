package com.twitter.finatra.benchmarks

import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.benchmarks.domain.TestImpressionTaskRequest
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.FinatraSerDeSimpleModule
import com.twitter.finatra.json.modules.FinatraJacksonModule
import java.io.ByteArrayInputStream
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.profile.StackProfiler
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, TimeValue}

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
    finatraObjectMapper.parse[TestImpressionTaskRequest](is)
  }

  @Benchmark
  def jacksonScalaModuleCaseClassDeserializer() = {
    val is = new ByteArrayInputStream(bytes)
    jacksonScalaModuleObjectMapper.parse[TestImpressionTaskRequest](is)
  }
}

object JsonBenchmarkMain {
  def main(args: Array[String]) {
    new Runner(new OptionsBuilder()
      .include(".*JsonBenchmark.*")
      .warmupIterations(10)
      .warmupTime(TimeValue.seconds(1))
      .measurementIterations(10)
      .measurementTime(TimeValue.seconds(2))
      .forks(1)
      .addProfiler(classOf[StackProfiler])
      .jvmArgsAppend("-Djmh.stack.period=1")
      .build()).run()
  }
}

object JsonBenchmarkProfilingMain {
  def main(args: Array[String]) {
    val jsonBenchmark = new JsonBenchmark()
    println("Starting")
    while (true) {
      jsonBenchmark.finatraCustomCaseClassDeserializer()
    }
  }
}