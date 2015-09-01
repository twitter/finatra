package com.twitter.inject.app.tests.internal

import com.twitter.inject.app.EmbeddedApp
import com.twitter.inject.app.tests.SampleGuiceApp
import com.twitter.inject.{Test, TwitterModule}
import com.twitter.util.Await

class InternalAppIntegrationTest extends Test {

  "start app" in {
    val app = new EmbeddedApp(
      new SampleGuiceApp {
        addFrameworkModule(FooModule)

        override def appMain(): Unit = {
          super.appMain()
          assert(injector.instance[Foo].name == "bar")
        }
      },
      waitForWarmup = true,
      skipAppMain = true)

    app.start()
    app.appMain()

    Await.result(
      app.mainResult)

    app.close()
  }

  "call injector before main" in {
    val e = intercept[Exception] {
      new EmbeddedApp(
        new SampleGuiceApp {
          addFrameworkModules(FooModule)
          injector.instance[Foo]
        },
        waitForWarmup = true,
        skipAppMain = false)
    }
    e.getMessage should startWith("injector is not available")
  }

  "error in appMain" in {
    val app = new EmbeddedApp(
      new SampleGuiceApp {
        override def appMain(): Unit = {
          super.appMain()
          throw new Exception("oops")
        }
      },
      waitForWarmup = true,
      skipAppMain = false)

    val e = intercept[Exception] {
      app.start()
    }
    e.getMessage should startWith("oops")
  }
}

object FooModule extends TwitterModule {
  override def configure: Unit = {
    bind[Foo].toInstance(new Foo("bar"))
  }
}

case class Foo(name: String)