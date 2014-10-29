package com.twitter.finatra

import com.google.inject.Module
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Response, Request => FinagleRequest}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.modules.{AccessLogModule, CallbackConverterModule, MessageBodyModule, MustacheModule}
import com.twitter.finatra.routing.AdminUtils.addAdminRoutes
import com.twitter.finatra.routing.Router
import com.twitter.finatra.server.FinatraHttpServer
import com.twitter.finatra.twitterserver.Handler

trait FinatraServer extends FinatraHttpServer {

  addFrameworkModules(
    accessLogModule,
    mustacheModule,
    messageBodyModule,
    jacksonModule,
    callbackModule)

  /* Abstract */

  protected def configure(router: Router)

  /* Overrides */

  override protected def postStartup() {
    super.postStartup()
    val router = injector.instance[Router]
    configure(router)
  }

  override def httpService: Service[FinagleRequest, Response] = {
    val router = injector.instance[Router]
    addAdminRoutes(router.services.adminService)
    router.services.externalService
  }

  /* Protected */

  protected def run[T <: Handler : Manifest]() {
    injector.instance[T].handle()
  }

  //TODO: Replace the need for these protected methods with "Guice v4" OptionalBinder
  //http://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/multibindings/OptionalBinder.html
  protected def accessLogModule: Module = AccessLogModule
  protected def mustacheModule: Module = MustacheModule
  protected def messageBodyModule: Module = MessageBodyModule
  protected def jacksonModule: Module = FinatraJacksonModule
  protected def callbackModule: Module = CallbackConverterModule
}
