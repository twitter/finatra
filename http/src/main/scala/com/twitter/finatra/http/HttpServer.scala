package com.twitter.finatra.http

import com.google.inject.Module
import com.twitter.finagle._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.internal.routing.AdminHttpRouter
import com.twitter.finatra.http.internal.server.BaseHttpServer
import com.twitter.finatra.http.modules._
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.annotations.Lifecycle

/** AbstractHttpServer for usage from Java */
abstract class AbstractHttpServer extends HttpServer

trait HttpServer extends BaseHttpServer {

  /** Add Framework Modules */
  addFrameworkModules(
    accessLogModule,
    DocRootModule,
    ExceptionManagerModule,
    jacksonModule,
    messageBodyModule,
    mustacheModule)

  /* Abstract */

  protected def configureHttp(router: HttpRouter): Unit

  /* Lifecycle */

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()
    val httpRouter = injector.instance[HttpRouter]
    configureHttp(httpRouter)
  }

  /* Overrides */

  override final def httpService: Service[Request, Response] = {
    val router = injector.instance[HttpRouter]
    AdminHttpRouter.addAdminRoutes(this, router, this.routes)
    router.services.externalService
  }

  /* Protected */

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.twitter.finagle.filter.LogFormatter]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.twitter.finagle.filter.LogFormatter]] implementation.
   */
  protected def accessLogModule: Module = AccessLogModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.github.mustachejava.MustacheFactory]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.github.mustachejava.MustacheFactory]] implementation.
   */
  protected def mustacheModule: Module = MustacheModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing implementations for a
   * [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]] and a
   * [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides implementations for
   *         [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]] and
   *         [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
   */
  protected def messageBodyModule: Module = MessageBodyModule

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.twitter.finatra.json.FinatraObjectMapper]].
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.twitter.finatra.json.FinatraObjectMapper]] implementation.
   */
  protected def jacksonModule: Module = FinatraJacksonModule
}
