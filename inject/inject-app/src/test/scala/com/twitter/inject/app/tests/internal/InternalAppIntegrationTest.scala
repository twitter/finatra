package com.twitter.inject.app.tests.internal

import com.twitter.inject.app.EmbeddedApp
import com.twitter.inject.app.tests.SampleApp
import com.twitter.inject.{Test, TwitterModule}

class InternalAppIntegrationTest extends Test {

  test("start app") {
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
}

object FooModule extends TwitterModule {
  override def configure(): Unit = {
    bind[Foo].toInstance(new Foo("bar"))
  }
}

case class Foo(name: String)