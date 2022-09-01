package com.twitter.inject.server.tests

import com.google.inject.name.Names
import com.google.inject.Module
import com.google.inject.Provides
import com.google.inject.Stage
import com.twitter.finagle.http.Status
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.tracing.Tracer
import com.twitter.inject.app.StartupTimeoutException
import com.twitter.inject.app.TestConsoleWriter
import com.twitter.inject.server.EmbeddedTwitterServer.ReducibleFn
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.inject.server.TwitterServer
import com.twitter.inject.Test
import com.twitter.inject.TwitterModule
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Promise
import com.twitter.util.TimeoutException
import javax.inject.Singleton
import org.scalatest.exceptions.TestFailedException
import scala.collection.immutable.ListMap
import scala.util.Random

class EmbeddedTwitterServerIntegrationTest extends Test {

  test("server#start") {
    val twitterServer = new TwitterServer {
      override def start(): Unit = {
        injector.instance[StatsReceiver].counter("test/counter").incr()
      }
    }
    twitterServer.addFrameworkOverrideModules(new TwitterModule {})
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = twitterServer,
      disableTestLogging = true
    )

    try {
      embeddedServer.httpGetAdmin("/health", andExpect = Status.Ok, withBody = "OK\n")
      embeddedServer.inMemoryStats.counters.get("no/count") should be(None) // doesn't exist

      embeddedServer.inMemoryStats.counters.waitFor("test/counter", 1L)
      intercept[org.scalatest.exceptions.TestFailedDueToTimeoutException] {
        // the counter will never have this value.
        embeddedServer.inMemoryStats.counters.waitFor("test/counter", 11L)
      }
    } finally {
      embeddedServer.close()
    }
  }

  test("server#startup timeout") {
    // hangs indefinitely in premain
    val twitterServer = new TwitterServer {
      premain {
        Await.result(Future.never)
      }
    }

    val timeoutSeconds = 2
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = twitterServer,
      disableTestLogging = true,
      maxStartupTimeSeconds = timeoutSeconds
    )

    val e = intercept[StartupTimeoutException] {
      embeddedServer.start()
    }
    e.getMessage.contains(s"failed to startup within $timeoutSeconds seconds.") should be(true)
    e.getCause should not be null
    e.getCause.getClass.isAssignableFrom(classOf[TimeoutException]) should be(true)
  }

  test("server#assert healthy false with exception") {
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = new TwitterServer {},
      disableTestLogging = true
    )
    try {
      // asserting that a server that started OK is not healthy will blow up
      intercept[TestFailedException] {
        embeddedServer.assertHealthy(healthy = false)
      }
    } finally {
      embeddedServer.close()
    }
  }

  test("server#assert healthy false") {
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = new TwitterServer {
        override def afterPostWarmup(): Unit = {
          // do nothing -- we never report healthy
        }
      },
      disableTestLogging = true
    )
    try {
      // server never reports self as healthy
      embeddedServer.assertHealthy(healthy = false)
    } finally {
      embeddedServer.close()
    }
  }

  test("server#non-injectable start") {
    val twitterServer = new NonInjectionTestServer()
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = twitterServer,
      args = Seq("http.port=:0"),
      disableTestLogging = true
    )

    try {
      embeddedServer.httpGetAdmin("/health", andExpect = Status.Ok, withBody = "OK\n")

      intercept[IllegalStateException] {
        // we have no way to give you something useful here as the server is not injectable
        // and no override was provided.
        embeddedServer.statsReceiver
      }
    } finally {
      embeddedServer.close()
    }
  }

  test("server#custom stats receiver") {
    val counterName = Random.alphanumeric.take(18).mkString
    val testStatsReceiver = new TestStatsReceiver
    val twitterServer = new TwitterServer {
      override def start(): Unit = {
        injector.instance[StatsReceiver].counter(counterName).incr()
      }
    }
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = twitterServer,
      disableTestLogging = true,
      statsReceiverOverride = Some(testStatsReceiver)
    )

    try {
      embeddedServer.httpGetAdmin("/health", andExpect = Status.Ok, withBody = "OK\n")

      intercept[IllegalStateException] {
        embeddedServer.inMemoryStatsReceiver
      }

      intercept[IllegalStateException] {
        embeddedServer.inMemoryStats.print()
      }

      intercept[IllegalStateException] {
        embeddedServer.countersMap
      }

      intercept[IllegalStateException] {
        embeddedServer.statsMap
      }

      intercept[IllegalStateException] {
        embeddedServer.gaugeMap
      }

      intercept[IllegalStateException] {
        embeddedServer.clearStats()
      }

      assert(embeddedServer.statsReceiver.isInstanceOf[TestStatsReceiver])
    } finally {
      embeddedServer.close()
    }

    assert(testStatsReceiver.counters.nonEmpty) /* we add a counter in startup of the server */
  }

  test("server#custom stats receiver with non-injectable server") {
    val testStatsReceiver = new TestStatsReceiver
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = new NonInjectionTestServer(Some(testStatsReceiver)),
      args = Seq("http.port=:0"),
      disableTestLogging = true,
      statsReceiverOverride = Some(testStatsReceiver)
    )

    try {
      embeddedServer.httpGetAdmin("/health", andExpect = Status.Ok, withBody = "OK\n")

      intercept[IllegalStateException] {
        embeddedServer.inMemoryStatsReceiver
      }

      intercept[IllegalStateException] {
        embeddedServer.inMemoryStats.print()
      }

      intercept[IllegalStateException] {
        embeddedServer.countersMap
      }

      intercept[IllegalStateException] {
        embeddedServer.statsMap
      }

      intercept[IllegalStateException] {
        embeddedServer.gaugeMap
      }

      intercept[IllegalStateException] {
        embeddedServer.clearStats()
      }

      assert(embeddedServer.statsReceiver.isInstanceOf[TestStatsReceiver])
    } finally {
      embeddedServer.close()
    }

    assert(
      embeddedServer.statsReceiver == testStatsReceiver
    )
  }

  test("server#in memory stats receiver with non-injectable server") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = new NonInjectionTestServer(Some(inMemoryStatsReceiver)),
      args = Seq("http.port=:0"),
      disableTestLogging = true,
      statsReceiverOverride = Some(inMemoryStatsReceiver)
    )

    try {
      embeddedServer.httpGetAdmin("/health", andExpect = Status.Ok, withBody = "OK\n")

      // this should not blow up
      embeddedServer.inMemoryStatsReceiver

      assert(embeddedServer.statsReceiver.isInstanceOf[InMemoryStatsReceiver])
    } finally {
      embeddedServer.printStats()
      embeddedServer.close()
    }

    assert(
      inMemoryStatsReceiver == embeddedServer.inMemoryStatsReceiver
    )
  }

  test("server#fail if server is a singleton") {
    intercept[IllegalArgumentException] {
      new EmbeddedTwitterServer(SingletonServer, disableTestLogging = true)
    }
  }

  test("server#fail if bind on a non-injectable server") {
    intercept[IllegalStateException] {
      new EmbeddedTwitterServer(
        twitterServer = new NonInjectionTestServer(),
        args = Seq("http.port=:0"),
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
      e.getMessage.contains("Error parsing flag \"foo.bar\"") should be(true)
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

      e.getMessage.contains("Error parsing flag \"foo.bar\"") should be(true)
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
        ) {
          override protected[twitter] def withLocals(fn: => Unit): Unit =
            com.twitter.finagle.stats.logOnShutdown.let(true)(super.withLocals(fn))
        }.bind[String].toInstance("helloworld")

      try {
        server.start() // start the server, otherwise the scope will never be entered
        shouldLogMetrics should equal(true) // verify mutation of inner scope
        com.twitter.finagle.stats
          .logOnShutdown() should equal(false) // verify outer scope is not changed
      } finally {
        server.close()
      }
    }
    com.twitter.finagle.stats.logOnShutdown() should equal(false) //verify default value unchanged
  }

  test("server#support disabling AdminHttpServer") {
    @volatile var awaitablesContainAdmin: Boolean = true
    val server =
      new EmbeddedTwitterServer(
        twitterServer = new TwitterServer {
          override val disableAdminHttpServer: Boolean = true

          override def postWarmup(): Unit = {
            // we need to verify that the admin is not started or expecting closure
            awaitablesContainAdmin = awaitables.contains(adminHttpServer)
            super.postWarmup()
          }
        },
        disableTestLogging = true
      ).bind[String].toInstance("helloworld")

    try {
      server.start()
      awaitablesContainAdmin should equal(false) // verify that we never started the adminHttpServer
    } finally {
      server.close()
    }
  }

  test("server#supports validating traces") {
    val promise = new Promise[Unit]
    // NOTE: this is NOT a realistic example, as tracing is tightly integrated with a Finagle Client or Server
    // implementation. Tracing is attached to a request/response lifecycle, which we are not illustrating here.
    // We are only verifying that the InMemoryTracer is correctly wired up to the EmbeddedServer and scoping the
    // Tracer in a manner that is consistent with a Finagle Client or Server TraceInitializerFilter so that we can
    // verify the utility of the Trace tool. There should be other tests with the protocol specific embedded servers
    // that are more realistic.
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = new TwitterServer {
        override def start(): Unit = {
          Trace.letTracer(injector.instance[Tracer]) {
            val trace = Trace()
            if (trace.isActivelyTracing) {
              trace.record("Hello, World!")
              trace.recordBinary("Hello", "World!")
            }
            promise.setDone()
            super.start()
          }
        }
      }
    )
    try {
      embeddedServer.start()
      await(promise) // need to wait until the trace has been annotated
      embeddedServer.usesInMemoryTracer should equal(true)
      embeddedServer.inMemoryTracer.binaryAnnotations.get("World").isDefined should equal(false)
      embeddedServer.inMemoryTracer.binaryAnnotations("Hello")
      intercept[IllegalArgumentException] {
        embeddedServer.inMemoryTracer.binaryAnnotations("World")
      }
      intercept[IllegalArgumentException] {
        embeddedServer.inMemoryTracer.binaryAnnotations("World", "Hello")
      }
      embeddedServer.inMemoryTracer.messages("Hello, World!")
      embeddedServer.inMemoryTracer.messages.get("World, Hello?").isDefined should equal(false)
      intercept[IllegalArgumentException] {
        embeddedServer.inMemoryTracer.messages("World")
      }
      embeddedServer.inMemoryTracer.binaryAnnotations("Hello", "World!")
      embeddedServer.inMemoryTracer.binaryAnnotations.get("World", "Hello?").isDefined should equal(
        false)
      embeddedServer.inMemoryTracer.messages.get("Goodbye, All").isDefined should equal(false)
      embeddedServer.inMemoryTracer.rpcs.get("rpc").isDefined should equal(false)
      intercept[IllegalArgumentException] {
        embeddedServer.inMemoryTracer.rpcs("rpc")
      }
      embeddedServer.inMemoryTracer.serviceNames.get("name").isDefined should equal(false)
      intercept[IllegalArgumentException] {
        embeddedServer.inMemoryTracer.serviceNames("name")
      }

      // look up a trace that isn't present and verify that we print the trace information
      val console = new TestConsoleWriter()
      console.let {
        val exception = intercept[IllegalArgumentException] {
          embeddedServer.inMemoryTracer.serviceNames("http")
        }
        exception.getMessage.contains(
          "ServiceName Annotation with name 'http' does not exist") should equal(true)
      }
      val out = console.inspectOut()
      out.contains("BinaryAnnotation(Hello,World!)") should equal(true)
      out.contains("Message(Hello, World!)") should equal(true)

    } finally {
      embeddedServer.close()
    }
  }

  test("server#supports overriding the InMemoryTracer") {
    val promise = new Promise[Unit]
    // NOTE: this is NOT a realistic example, as tracing is tightly integrated with a Finagle Client or Server
    // implementation. Tracing is attached to a request/response lifecycle, which we are not illustrating here.
    // We are only verifying that the InMemoryTracer is correctly wired up to the EmbeddedServer and scoping the
    // Tracer in a manner that is consistent with a Finagle Client or Server TraceInitializerFilter so that we can
    // verify the utility of the Trace tool. There should be other tests with the protocol specific embedded servers
    // that are more realistic.
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = new TwitterServer {
        override def start(): Unit = {
          Trace.letTracer(injector.instance[Tracer]) {
            val trace = Trace()
            if (trace.isActivelyTracing) {
              trace.record("Hello, World!")
              trace.recordBinary("Hello", "World!")
            }
            promise.setDone()
            super.start()
          }
        }
      },
      tracerOverride = Some(NullTracer)
    )
    try {
      embeddedServer.start()
      await(promise) // need to wait until the trace has been annotated
      embeddedServer.usesInMemoryTracer should equal(false)
      intercept[IllegalStateException] {
        embeddedServer.inMemoryTracer.binaryAnnotations.get("World").isDefined should equal(false)
        embeddedServer.inMemoryTracer.binaryAnnotations("Hello")
        embeddedServer.inMemoryTracer.messages("Hello, World!")
        embeddedServer.inMemoryTracer.binaryAnnotations("Hello", "World!")
        embeddedServer.inMemoryTracer.messages.get("Goodbye, All").isDefined should equal(false)
      }
    } finally {
      embeddedServer.close()
    }
  }

  test("server#httpPostAdmin sends a valid POST request") {
    val embeddedServer = new EmbeddedTwitterServer(
      twitterServer = new TwitterServer {}
    )
    try {
      embeddedServer.httpPostAdmin("/quitquitquit", andExpect = Status.Ok, withBody = "quitting\n")
    } finally {
      embeddedServer.close()
    }
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
    sb.toString should equal(
      "321abc"
    ) //check that ordering of execution is correct (fn is inner-most)
  }
}

object SingletonServer extends TwitterServer
