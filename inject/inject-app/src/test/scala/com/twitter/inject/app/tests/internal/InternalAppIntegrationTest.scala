package com.twitter.inject.app.tests.internal

import com.twitter.inject.app.EmbeddedApp
import com.twitter.inject.app.tests.SampleApp
import com.twitter.inject.{Test, TwitterModule}
import com.twitter.util.Await

class InternalAppIntegrationTest extends Test {

  "start app" in {
    val app = new EmbeddedApp(
      new SampleApp {
        addFrameworkModule(FooModule)

        override def appMain(): Unit = {
          super.appMain()
          assert(injector.instance[Foo].name == "bar")
        }
      },
      waitForWarmup = true,
      skipAppMain = true,
      verbose = false)

    app.start()
    app.appMain()

    Await.result(
      app.mainResult)

    app.close()
  }

  "call injector before main" in {
    val e = intercept[Exception] {
      new SampleApp {
        addFrameworkModules(FooModule)
        injector.instance[Foo]
      }
    }
    e.getMessage should startWith("injector is not available")
  }

  "error in appMain" in {
    val app = new SampleApp {
      override def appMain(): Unit = {
        super.appMain()
        throw new scala.Exception("oops")
      }
    }

    val e = intercept[Exception] {
      app.main()
    }

    app.close()
    e.getMessage should startWith("oops")
  }

  "two apps starting" in {
    val a = new EmbeddedApp(new com.twitter.inject.app.App {})
    a.start()
    a.close()

    val b = new EmbeddedApp(new com.twitter.inject.app.App {})
    b.start()
    b.close()
  }
}

object FooModule extends TwitterModule {
  override def configure(): Unit = {
    bind[Foo].toInstance(new Foo("bar"))
  }
}

case class Foo(name: String)