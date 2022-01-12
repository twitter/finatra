package com.twitter.finatra.http.tests
package integration.routing

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.marshalling.modules.MessageBodyFlagsModule
import com.twitter.finatra.http.modules.AccessLogModule
import com.twitter.finatra.http.modules.ExceptionManagerModule
import com.twitter.finatra.http.modules.MessageBodyModule
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.finatra.modules.FileResolverModule
import com.twitter.finatra.validation.ValidatorModule
import com.twitter.inject.InjectorModule
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.modules.internal.LibraryModule

class HttpRouterIntegrationTest extends Test {

  private[this] def mkInjector: TestInjector = {
    TestInjector(
      AccessLogModule,
      FileResolverModule,
      ExceptionManagerModule,
      InjectorModule,
      new LibraryModule("finatra"),
      ScalaObjectMapperModule,
      MessageBodyFlagsModule,
      MessageBodyModule,
      StatsReceiverModule,
      ValidatorModule
    ).create()
  }

  test("Empty HttpRouter returns an external service with no routes") {
    val injector: TestInjector = mkInjector

    try {
      val router = injector.instance[HttpRouter]

      router.services.externalService should equal(router.externalService)

      val svc = router.externalService
      val response = await(svc(Request("/foo"))) // HttpRouter has no routes
      response.status should equal(Status.NotFound)
    } finally {
      injector.close()
    }
  }

  test("Accessing external service before configuration memoizes empty service") {
    val injector: TestInjector = mkInjector

    try {
      val router = injector.instance[HttpRouter]

      val svc = router.externalService
      val response = await(svc(Request("/foo"))) // HttpRouter has no routes
      response.status should equal(Status.NotFound)

      // configuring the router now has no effect on the returned service since it has been memoized as "empty"
      router.add(new Controller {
        get("/foo") { _: Request => "bar" }
      })

      await(svc(Request("/foo"))).status should equal(Status.NotFound)

      val svc2 = router.externalService // doesn't matter calling it again, same result
      await(svc2(Request("/foo"))).status should equal(Status.NotFound)
    } finally {
      injector.close()
    }
  }

  test("Configuring the router before accessing the external service works properly") {
    val injector: TestInjector = mkInjector

    try {
      val router = injector.instance[HttpRouter]
      router.add(new Controller {
        get("/foo") { _: Request => "bar" }
      })

      val svc = router.externalService
      val response = await(svc(Request("/foo")))
      response.status should equal(Status.Ok)
      response.contentString should be("bar")
    } finally {
      injector.close()
    }
  }
}
