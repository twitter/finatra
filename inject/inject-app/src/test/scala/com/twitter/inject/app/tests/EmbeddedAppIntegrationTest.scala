package com.twitter.inject.app.tests

import com.google.inject.Module
import com.google.inject.Provides
import com.google.inject.Stage
import com.twitter.inject.Test
import com.twitter.inject.TwitterModule
import com.twitter.inject.app.App
import com.twitter.inject.app.EmbeddedApp
import com.twitter.inject.app.TestConsoleWriter
import com.twitter.inject.app.console.ConsoleWriter
import com.twitter.util.logging.Logging
import com.twitter.util.mock.Mockito
import javax.inject.Inject
import javax.inject.Singleton

class EmbeddedAppIntegrationTest extends Test with Mockito {

  test("start app") {
    val sampleApp = new SampleApp
    val app = new EmbeddedApp(sampleApp)

    app.main()

    sampleApp.sampleServiceResponse should be("hi yo")
  }

  test("exception in App#run() throws") {
    val app = new EmbeddedApp(new SampleApp {
      override protected def run(): Unit = {
        throw new RuntimeException("FORCED EXCEPTION")
      }
    })

    intercept[Exception] {
      app.main()
    }
  }

  test("start app with FooModule") {
    val app = new EmbeddedApp(new SampleApp {
      addFrameworkModule(FooModule)

      override protected def run(): Unit = {
        super.run()
        assert(injector.instance[Foo].name == "bar")
      }
    })

    app.main()
  }

  test("call injector before main") {
    val e = intercept[Exception] {
      new SampleApp {
        addFrameworkModules(FooModule)
        injector.instance[Foo]
      }
    }
    e.getMessage should startWith("injector is not available")
  }

  test("error in run fails startup") {
    val app = new SampleApp {
      override protected def run(): Unit = {
        super.run()
        throw new scala.Exception("oops")
      }
    }

    intercept[Exception] {
      app.main()
    }
  }

  test("two apps starting") {
    val a = new EmbeddedApp(new com.twitter.inject.app.App {})
    a.main()

    val b = new EmbeddedApp(new com.twitter.inject.app.App {})
    b.main()
  }

  test("bind") {
    val mockSampleService = mock[SampleService]
    mockSampleService.sayHi(any[String]) returns "hi mock"

    val sampleApp = new SampleApp

    val app = new EmbeddedApp(sampleApp).bind[SampleService].toInstance(mockSampleService)
    app.main()

    sampleApp.sampleServiceResponse should be("hi mock")
  }

  test("failfastOnFlagsNotParsed") {
    // Should NOT throw an IllegalStateException, even though we eagerly read the `f` flag as we set
    // fail fast to false.
    val app = new EmbeddedApp(new FailfastOnFlagsNotParsedApp(false))
    app.main()
  }

  test("failfastOnFlagsNotParsed fails properly") {
    // Throws an IllegalStateException, because we eagerly read the `f` flag and set fail fast to true.
    intercept[IllegalStateException] {
      val app = new EmbeddedApp(new FailfastOnFlagsNotParsedApp(true))
      app.main()
    }
  }

  test("access console output of an app using the ConsoleWriter directly") {
    val console = new TestConsoleWriter()
    val app = new EmbeddedApp(new App {
      override def run(): Unit = {
        val console = injector.instance[ConsoleWriter]
        console.out.println("Hello, World!")
        console.err.println("Oh, No!")
      }
    }).bind[ConsoleWriter].toInstance(console)

    assert(console.inspectOut() == "") // main hasn't been run yet, we don't have any output
    assert(console.inspectErr() == "") // main hasn't been run yet, we don't have any output
    app.main()
    assert(console.inspectOut() == "Hello, World!\n")
    assert(console.inspectErr() == "Oh, No!\n")
  }

  test("access console output of an app using the ConsoleWriter via implicit scope") {
    val console = new TestConsoleWriter()

    val app = new EmbeddedApp(new App {
      override def run(): Unit = {
        print("Hello, World:")
        Console.err.println("Oof!") // will be captured
        Console.out.println(" Part 2!") // will be captured
        System.out.println("ABC") // will NOT be captured
        System.err.println("XYZ") // will NOT be captured
        info("INFO log") // will NOT be captured
        error("ERROR log") // will NOT be captured
      }
    }).bind[ConsoleWriter].toInstance(console)

    app.main()

    println("Test that we've escaped scope!") // should not get captured, not part of our App

    assert(console.inspectOut() == "Hello, World: Part 2!\n")
    assert(console.inspectErr() == "Oof!\n")
  }

  test("app#injector error") {
    val sampleApp = new SampleApp
    val app = new EmbeddedApp(sampleApp, stage = Stage.PRODUCTION)

    intercept[Exception] {
      app.main()
    }.getCause.getMessage should be("Yikes")
  }
}

object SampleAppMain extends SampleApp

/* an app that eagerly applies (reads) a defined flag, with a toggle
   to fail or not on read before parse */
class FailfastOnFlagsNotParsedApp(fail: Boolean = true) extends App {
  // this is not an example to follow. this value should always be 'true'.
  override protected def failfastOnFlagsNotParsed: Boolean = fail
  private val f = flag("testing", "1", "help")
  f()
}

class SampleApp extends App {
  var sampleServiceResponse: String = ""

  override val name = "sample-app"

  override val modules: Seq[Module] = Seq(new TwitterModule() {
    @Provides
    @Singleton
    def providesFoo: Integer = {
      throw new Exception("Yikes")
    }
  })

  override protected def run(): Unit = {
    sampleServiceResponse = injector.instance[SampleManager].start()
  }
}

class SampleManager @Inject() (sampleService: SampleService) extends Logging {
  def start(): String = {
    info("SampleManager started")
    val response = sampleService.sayHi("yo")
    info("Service said " + response)
    response
  }
}

class SampleService {
  def sayHi(msg: String): String = {
    "hi " + msg
  }
}

object FooModule extends TwitterModule {
  override def configure(): Unit = {
    bind[Foo].toInstance(Foo("bar"))
  }
}

case class Foo(name: String)
