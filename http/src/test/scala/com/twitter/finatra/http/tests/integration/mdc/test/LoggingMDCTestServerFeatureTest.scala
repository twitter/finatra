package com.twitter.finatra.http.tests.integration.mdc.test

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.tracing.{Flags, Trace, TraceId}
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

  test("MDC logging with MDC filter with trace sampling undecided") {
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

    val traceId = Trace.nextId
    Trace.letId(traceId) {
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
        controller.getStoredMDC.get.size should equal(4)
        controller.getStoredMDC.get("userId") should not be null
        controller.getStoredMDC.get("userId") should be("1234")

        controller.getStoredMDC.get("traceId") should not be null
        controller.getStoredMDC.get("traceId") should be(traceId.traceId.toString())

        controller.getStoredMDC.get("traceSpanId") should not be null
        // A new span Id would be created for the client call.
        controller.getStoredMDC.get("traceSpanId") should not be (traceId.traceId.toString)

        controller.getStoredMDC.get("traceSampled") should not be null
        controller.getStoredMDC.get("traceSampled") should be("false")

      } finally {
        server.close()
      }
    }
  }
  test("MDC logging with MDC filter with sampled set to true") {
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

    val tempTraceId = Trace.nextId
    val traceId = TraceId(None, None, tempTraceId.spanId, None, Flags().setDebug, None)
    Trace.letId(traceId) {
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
        controller.getStoredMDC.get.size should equal(4)
        controller.getStoredMDC.get("userId") should not be null
        controller.getStoredMDC.get("userId") should be("1234")

        controller.getStoredMDC.get("traceId") should not be null
        controller.getStoredMDC.get("traceId") should be(traceId.traceId.toString())

        controller.getStoredMDC.get("traceSpanId") should not be null
        // A new span Id would be created for the client call.
        controller.getStoredMDC.get("traceSpanId") should not be (traceId.traceId.toString)

        controller.getStoredMDC.get("traceSampled") should not be null
        controller.getStoredMDC.get("traceSampled") should be("true")

      } finally {
        server.close()
      }
    }
  }
}
