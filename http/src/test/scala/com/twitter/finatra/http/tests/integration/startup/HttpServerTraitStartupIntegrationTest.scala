package com.twitter.finatra.http.tests.integration.startup

import com.google.inject.Module
import com.twitter.finagle.Service
import com.twitter.finagle.http.service.NullService
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.modules.MessageBodyModule
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServerTrait}
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.finatra.modules.FileResolverModule
import com.twitter.inject.Test
import com.twitter.inject.modules.StatsReceiverModule

class HttpServerTraitStartupIntegrationTest extends Test {

  test("HttpServerTrait startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServerTrait {
        override val modules: Seq[Module] = Seq(
          ScalaObjectMapperModule,
          FileResolverModule,
          MessageBodyModule,
          StatsReceiverModule)

        /** Override with an implementation to serve an HTTP Service */
        override protected def httpService: Service[Request, Response] = NullService
      },
      disableTestLogging = true
    )

    try {
      server.start()
      server.assertHealthy()
    } finally {
      server.close()
    }
  }

  test("HttpServerTrait startup with only an http external port and no admin port") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServerTrait {
        override val disableAdminHttpServer = true
        override val modules: Seq[Module] = Seq(
          ScalaObjectMapperModule,
          FileResolverModule,
          MessageBodyModule,
          StatsReceiverModule)

        /** Override with an implementation to serve an HTTP Service */
        override protected def httpService: Service[Request, Response] = NullService
      },
      disableTestLogging = true)

    try {
      server.start()
      // Because we disabled the adminHttpServer we instead check the started flag.
      server.assertStarted()
    } finally {
      server.close()
    }
  }
}
