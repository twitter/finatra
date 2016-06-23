package com.twitter.inject.app.tests.internal

import com.twitter.inject.app.EmbeddedApp
import com.twitter.inject.app.tests.SampleApp
import com.twitter.inject.{Test, TwitterModule}

class InternalAppIntegrationTest extends Test {

  "start app" in {
    val app = new EmbeddedApp(
      new SampleApp {
        addFrameworkModule(FooModule)

        override protected def run(): Unit = {
          super.run()
          assert(injector.instance[Foo].name == "bar")
        }
      })

    app.main()
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

  "error in run fails startup" in {
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

  "two apps starting" in {
    val a = new EmbeddedApp(new com.twitter.inject.app.App {})
    a.main()

    val b = new EmbeddedApp(new com.twitter.inject.app.App {})
    b.main()
  }
}

object FooModule extends TwitterModule {
  override def configure(): Unit = {
    bind[Foo].toInstance(new Foo("bar"))
  }
}

case class Foo(name: String)