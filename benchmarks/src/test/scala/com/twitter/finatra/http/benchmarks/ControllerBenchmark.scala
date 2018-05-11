package com.twitter.finatra.http.benchmarks

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finatra.StdBenchAnnotations
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.filters.HttpResponseFilter
import com.twitter.finatra.http.modules._
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.inject.app.TestInjector
import com.twitter.util.Future
import org.openjdk.jmh.annotations.{Benchmark, Scope, State}

/**
 * ./sbt 'project benchmarks' 'jmh:run ControllerBenchmark'
 */
@State(Scope.Thread)
class ControllerBenchmark extends StdBenchAnnotations {

  val injector: Injector =
    TestInjector(
      flags = Map("http.response.charset.enabled" -> "false"),
      modules = Seq(
        ExceptionManagerModule,
        MessageBodyModule,
        FinatraJacksonModule,
        MustacheModule,
        DocRootModule,
        NullStatsReceiverModule
      )
    ).create

  val httpRouter: HttpRouter = injector.instance[HttpRouter]
  val httpService: Service[Request, Response] =
    httpRouter
      .filter[HttpResponseFilter[Request]]
      .add[PlaintextAndJsonController]
      .services
      .externalService

  @Benchmark
  def plaintext(): Future[Response] = {
    httpService(Request("/plaintext"))
  }

  @Benchmark
  def json(): Future[Response] = {
    httpService(Request("/json"))
  }
}

class PlaintextAndJsonController extends Controller {
  private[this] val helloWorldResponseText = "Hello, World!"

  get("/plaintext") { request: Request =>
    helloWorldResponseText
  }

  get("/json") { _: Request =>
    Map("message" -> "Hello, World!")
  }
}

object NullStatsReceiverModule extends TwitterModule {
  override def configure() {
    bindSingleton[StatsReceiver].toInstance(NullStatsReceiver)
  }
}
