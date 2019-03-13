package com.twitter.finatra.http.tests.integration.startup

import com.twitter.finagle.Service
import com.twitter.finagle.http.service.NullService
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.modules.ResponseBuilderModule
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServerTrait}
import com.twitter.inject.Test

class HttpServerTraitStartupIntegrationTest extends Test {

  test("HttpServerTrait startup") {
    val server = new EmbeddedHttpServer(twitterServer = new HttpServerTrait {
      override val modules = Seq(ResponseBuilderModule)

      /** Override with an implementation to serve an HTTP Service */
      override protected def httpService: Service[Request, Response] = NullService
    })

    server.start()
    server.assertHealthy()
    server.close()
  }

  test("HttpServerTrait startup with only an http external port and no admin port") {
    val server = new EmbeddedHttpServer(twitterServer = new HttpServerTrait {
      override val disableAdminHttpServer = true
      override val modules = Seq(ResponseBuilderModule)

      /** Override with an implementation to serve an HTTP Service */
      override protected def httpService: Service[Request, Response] = NullService
    })

    server.start()
    // Because we disabled the adminHttpServer we instead check the started flag.
    server.assertStarted()
    server.close()
  }
}
