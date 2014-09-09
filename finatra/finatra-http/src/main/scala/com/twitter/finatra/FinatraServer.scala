package com.twitter.finatra

import com.google.inject.Module
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Response, Request => FinagleRequest}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.twitterserver.Handler
import com.twitter.finatra.twitterserver.modules.{AccessLogModule, MessageBodyModule, MustacheModule}
import com.twitter.finatra.twitterserver.routing.AdminUtils.addAdminRoutes
import com.twitter.finatra.twitterserver.routing.Router

trait FinatraServer extends FinatraRawServer {

  addFrameworkModules(
    AccessLogModule,
    mustacheModule,
    messageBodyModule,
    jacksonModule)

  /* Abstract */

  protected def configure(router: Router)

  /* Overrides */

  override protected def postStartup() {
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

  //TODO: Replace the need for these methods with Guice v4 OptionalBinder
  //http://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/multibindings/OptionalBinder.html
  protected def mustacheModule: Module = MustacheModule
  protected def messageBodyModule: Module = MessageBodyModule
  protected def jacksonModule: Module = FinatraJacksonModule
}
