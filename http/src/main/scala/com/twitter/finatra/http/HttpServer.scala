package com.twitter.finatra.http

import com.google.inject.Module
import com.twitter.finagle._
import com.twitter.finagle.http.{HttpMuxer, Request, Response}
import com.twitter.finatra.http.internal.server.BaseHttpServer
import com.twitter.finatra.http.modules.{AccessLogModule, DocRootModule, ExceptionMapperModule, MessageBodyModule, MustacheModule}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule

trait HttpServer extends BaseHttpServer {

  addFrameworkModules(
    mustacheModule,
    messageBodyModule,
    exceptionMapperModule,
    jacksonModule,
    DocRootModule,
    accessLogModule)

  /* Abstract */

  protected def configureHttp(router: HttpRouter): Unit

  /* Overrides */

  override protected def failfastOnFlagsNotParsed = true

  override protected def postStartup() {
    super.postStartup()
    val httpRouter = injector.instance[HttpRouter]
    configureHttp(httpRouter)
  }

  override def httpService: Service[Request, Response] = {
    val router = injector.instance[HttpRouter]
    addAdminRoutes(router)
    router.services.externalService
  }

  /* Protected */

  protected def addAdminRoutes(router: HttpRouter) {
    HttpMuxer.addRichHandler(
      HttpRouter.FinatraAdminPrefix,
      router.services.adminService)
  }


  //Note: After upgrading to Guice v4, replace the need for these protected methods with OptionalBinder
  //http://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/multibindings/OptionalBinder.html
  protected def accessLogModule: Module = AccessLogModule

  protected def mustacheModule: Module = MustacheModule

  protected def messageBodyModule: Module = MessageBodyModule

  protected def exceptionMapperModule: Module = ExceptionMapperModule

  protected def jacksonModule: Module = FinatraJacksonModule
}
