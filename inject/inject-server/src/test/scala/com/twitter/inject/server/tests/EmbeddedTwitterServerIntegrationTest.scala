package com.twitter.inject.server.tests

import com.google.inject.name.Names
import com.google.inject.{Module, Provides, Stage}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Http, Service}
import com.twitter.inject.server.EmbeddedTwitterServer.ReducibleFn
import com.twitter.inject.server.{EmbeddedTwitterServer, TwitterServer}
import com.twitter.inject.{Logging, Test, TwitterModule}
import com.twitter.util.{Await, Future}
import javax.inject.Singleton
import scala.collection.immutable.ListMap

class EmbeddedTwitterServerIntegrationTest extends Test {

  test("server#start") {
    val twitterServer = new TwitterServer {}
    twitterServer.addFrameworkOverrideModules(new TwitterModule {})
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = twitterServer,
      disableTestLogging = true
    )

    try {
      embeddedServer.httpGetAdmin("/health", andExpect = Status.Ok, withBody = "OK\n")
    } finally {
      embeddedServer.close()
    }
  }

  test("server#fail if server is a singleton") {
    intercept[IllegalArgumentException] {
      new EmbeddedTwitterServer(SingletonServer)
    }
  }

  test("server#fail if bind on a non-injectable server") {
    intercept[IllegalStateException] {
      new EmbeddedTwitterServer(
        twitterServer = new NonInjectableServer,
        disableTestLogging = true
      ).bind[String].toInstance("hello!")
    }
  }

  test("server#support bind in server") {
    val server =
      new EmbeddedTwitterServer(
        twitterServer = new TwitterServer {},
        disableTestLogging = true
      ).bind[String].toInstance("helloworld")

    try {
      server.injector.instance[String] should be("helloworld")
    } finally {
      server.close()
    }
  }

  test("server#support bind with @Named in server") {
    val server =
      new EmbeddedTwitterServer(
        twitterServer = new TwitterServer {},
        disableTestLogging = true
      ).bind[String]
        .annotatedWith(Names.named("best"))
        .toInstance("helloworld")

    try {
      server.injector.instance[String](Names.named("best")) should be("helloworld")
    } finally {
      server.close()
    }
  }

  test("server#fail because of unknown flag") {
    val server = new EmbeddedTwitterServer(
      twitterServer = new TwitterServer {},
      flags = Map("foo.bar" -> "true"),
      disableTestLogging = true
    )

    try {
      val e = intercept[Exception] {
        server.assertHealthy()
      }
      e.getMessage.contains("Error parsing flag \"foo.bar\": flag undefined") should be(true)
    } finally {
      server.close()
    }
  }

  test("server#failed startup throws startup error on future method calls") {
    val server = new EmbeddedTwitterServer(
      twitterServer = new TwitterServer {},
      flags = Map("foo.bar" -> "true"),
      disableTestLogging = true
    )

    try {
      val e = intercept[Exception] {
        server.assertHealthy()
      }

      val e2 = intercept[Exception] { //accessing the injector requires a started server
        server.injector
      }

      e.getMessage.contains("Error parsing flag \"foo.bar\": flag undefined") should be(true)
      e.getMessage equals e2.getMessage
    } finally {
      server.close()
    }
  }

  test("server#injector error") {
    val server = new EmbeddedTwitterServer(
      stage = Stage.PRODUCTION,
      twitterServer = new TwitterServer {
        override val modules: Seq[Module] = Seq(new TwitterModule() {
          @Provides
          @Singleton
          def providesFoo: Integer = {
            throw new Exception("Yikes")
          }
        })
      },
      disableTestLogging = true
    ).bind[String].toInstance("helloworld")

    try {
      val e = intercept[Exception] {
        server.injector.instance[String] should be("helloworld")
      }
      e.getCause.getMessage should be("Yikes")
    } finally {
      server.close()
    }
  }

  test("server#support specifying GlobalFlags") {
    var shouldLogMetrics = false

    com.twitter.finagle.stats.logOnShutdown.let(false) { //set the scope of this test thread
      val server = new EmbeddedTwitterServer(
        twitterServer = new TwitterServer {
          override protected def postInjectorStartup(): Unit = {
            //mutate to match the inner scope of withLocals
            shouldLogMetrics = com.twitter.finagle.stats.logOnShutdown()
            super.postInjectorStartup()
          }
        },
        disableTestLogging = true,
        globalFlags = ListMap(
          com.twitter.finagle.stats.logOnShutdown -> "true"
        )
      )
      try {
        server.start() //start the server, otherwise the scope will never be entered
        shouldLogMetrics should equal(true) //verify mutation of inner scope
        com.twitter.finagle.stats
          .logOnShutdown() should equal(false) //verify outer scope is not changed
      } finally {
        server.close()
      }
    }
  }

  test("server#support local scope of underlying server") {
    var shouldLogMetrics = false

    com.twitter.finagle.stats.logOnShutdown() should equal(false) // verify initial default value

    com.twitter.finagle.stats.logOnShutdown.let(false) { // set the scope of this test thread
      val server =
        new EmbeddedTwitterServer(
          twitterServer = new TwitterServer {
            override protected def postInjectorStartup(): Unit = {
              // mutate to match the inner scope of withLocals
              shouldLogMetrics = com.twitter.finagle.stats.logOnShutdown()
              super.postInjectorStartup()
            }
          },
          disableTestLogging = true
        ){
          override protected[twitter] def withLocals(fn: => Unit): Unit =
            com.twitter.finagle.stats.logOnShutdown.let(true)(super.withLocals(fn))
        }.bind[String].toInstance("helloworld")

      try {
        server.start() // start the server, otherwise the scope will never be entered
        shouldLogMetrics should equal(true) // verify mutation of inner scope
        com.twitter.finagle.stats.logOnShutdown() should equal(false) // verify outer scope is not changed
      } finally {
        server.close()
      }
    }
    com.twitter.finagle.stats.logOnShutdown() should equal(false) //verify default value unchanged
  }

  /* utility method tests */

  test("method#reduceScopedFunction") {
    val sb = new StringBuilder

    val fns: Iterable[ReducibleFn[String]] = Seq(
      s => { sb.append(s); s },
      s => { sb.append('1'); s },
      s => { sb.append("2"); s },
      s => { sb.append("3"); s }
    )

    val fn = EmbeddedTwitterServer.mkGlobalFlagsFn(fns)
    fn("abc") should equal("abc") //check expected output of fn ("abc" is pass-thru)
    sb.toString should equal("321abc") //check that ordering of execution is correct (fn is inner-most)
  }

}

class NonInjectableServer extends com.twitter.server.TwitterServer with Logging {
  private[this] val service = new Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      val response = Response(request.version, Status.Ok)
      response.contentString = "hello"
      Future.value(response)
    }
  }

  def main(): Unit = {
    val server = Http.serve(":8888", service)
    onExit {
      server.close()
    }
    Await.ready(server)
  }
}

object SingletonServer extends TwitterServer
