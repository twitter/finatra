package com.twitter.finatra.http.tests.integration.mdc.test

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.tracing.{SpanId, Trace, TraceId}
import com.twitter.finatra.http.filters.{LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.tests.integration.mdc.main.LoggingMDCTestController
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.Test

class LoggingMDCTestServerFeatureTest extends Test {

  test("MDC logging") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router
            .filter[LoggingMDCFilter[Request, Response]]
            .add[LoggingMDCTestController]
        }
      }
    )
    try {
      server.httpPut(
        "/putUser/1234",
        putBody = "",
        andExpect = Status.Created,
        withLocation = "/user/1234"
      )

      server.httpPost(
        "/sendUserNotification/1234",
        postBody = "",
        andExpect = Status.Ok
      )

      val controller = server.injector.instance[LoggingMDCTestController]

      controller.getStoredMDC should not be None
      controller.getStoredMDC.get.size should equal(1)
      controller.getStoredMDC.get("userId") should not be null
      controller.getStoredMDC.get("userId") should be("1234")
    } finally {
      server.close()
    }
  }

  test("MDC logging with MDC filter") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router
            .filter[LoggingMDCFilter[Request, Response]]
            .filter[TraceIdMDCFilter[Request, Response]]
            .add[LoggingMDCTestController]
        }
      }
    )

    Trace.letId(Trace.nextId) {
      try {
        server.httpPut(
          "/putUser/1234",
          putBody = "",
          andExpect = Status.Created,
          withLocation = "/user/1234"
        )

        server.httpPost(
          "/sendUserNotification/1234",
          postBody = "",
          andExpect = Status.Ok
        )

        val controller = server.injector.instance[LoggingMDCTestController]

        controller.getStoredMDC should not be None
        controller.getStoredMDC.get.size should equal(2)
        controller.getStoredMDC.get("userId") should not be null
        controller.getStoredMDC.get("userId") should be("1234")
        controller.getStoredMDC.get("traceId") should not be null
      } finally {
        server.close()
      }
    }
  }
}
