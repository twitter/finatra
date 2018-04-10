package com.twitter.finatra.http.tests.integration.doeverything.test

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.request.HttpForward
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.tests.integration.doeverything.main.controllers._
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServer}
import com.twitter.inject.Test

class MaxRequestFowardingDepthTest extends Test {

  test("Default of 5 with infinite loop error on 6th call") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add(new ForwarderHelperController(injector.instance[HttpForward]))
            .add(new MaxForwardController(injector.instance[HttpForward]))
        }
      }
    )

    run(server) {
      val router = server.injector.instance[HttpRouter]
      router.maxRequestForwardingDepth should be(5)

      server.httpGet("/infinite", andExpect = InternalServerError)
    }
  }

  test("Default of 5 forwards with one request") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add(new ForwarderHelperController(injector.instance[HttpForward]))
            .add(new MaxForwardController(injector.instance[HttpForward]))
        }
      }
    )

    run(server) {
      val router = server.injector.instance[HttpRouter]
      router.maxRequestForwardingDepth should be(5)

      server.httpGet("/max/ok", andExpect = Ok)
    }
  }

  test("Default of 5 with 5 requests") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
           .add(new ForwarderHelperController(5, injector.instance[HttpForward]))
           .add(new MaxForwardController(5, injector.instance[HttpForward]))
        }
      }
    )

    run(server) {
      val router = server.injector.instance[HttpRouter]
      router.maxRequestForwardingDepth should be(5)

      server.httpGet("/max", andExpect = Ok)
    }

  }

  test("Custom max of 15 with infinite loop error on 16th call") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add(new ForwarderHelperController(injector.instance[HttpForward]))
            .add(new MaxForwardController(injector.instance[HttpForward]))
            .withMaxRequestForwardingDepth(15)
        }
      }
    )

    run(server) {
      val router = server.injector.instance[HttpRouter]
      router.maxRequestForwardingDepth should be(15)

      server.httpGet("/infinite", andExpect = InternalServerError)
    }
  }

  test("Custom max of 15 with one request") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add(new ForwarderHelperController(injector.instance[HttpForward]))
            .add(new MaxForwardController(injector.instance[HttpForward]))
            .withMaxRequestForwardingDepth(15)
        }
      }
    )

    run(server) {
      val router = server.injector.instance[HttpRouter]
      router.maxRequestForwardingDepth should be(15)

      server.httpGet("/max/ok", andExpect = Ok)
    }

  }
  test("Custom max of 15 with 15 requests") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add(new ForwarderHelperController(15, injector.instance[HttpForward]))
            .add(new MaxForwardController(15, injector.instance[HttpForward]))
            .withMaxRequestForwardingDepth(15)
        }
      }
    )

    run(server) {
      val router = server.injector.instance[HttpRouter]
      println(router.maxRequestForwardingDepth)
      router.maxRequestForwardingDepth should be(15)

      server.httpGet("/max", andExpect = Ok)
    }

  }

  // non positive numbers

  test("Negative max forwards prevents server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add(new ForwarderHelperController(injector.instance[HttpForward]))
            .add(new MaxForwardController(injector.instance[HttpForward]))
            .withMaxRequestForwardingDepth(-5)
        }
      }
    )
    run(server) {
      val e = intercept[IllegalArgumentException] {
        server.start()
      }

      e.getMessage should be("requirement failed: Maximum request forwarding depth: -5, must be greater than zero.")
    }
  }

  test("Zero max forwards prevents server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add(new ForwarderHelperController(injector.instance[HttpForward]))
            .add(new MaxForwardController(injector.instance[HttpForward]))
            .withMaxRequestForwardingDepth(0)
        }
      }
    )
    run(server) {
      val e = intercept[IllegalArgumentException] {
        server.start()
      }

      e.getMessage should be("requirement failed: Maximum request forwarding depth: 0, must be greater than zero.")
    }
  }

  private[this] def run(server: EmbeddedHttpServer)(fn: => Unit): Unit = {
    try {
      fn
    } finally {
      server.close()
    }
  }
}
