package com.twitter.inject.app.tests

import com.twitter.inject.app.{EmbeddedApp, FeatureTest, StartupTimeoutException}
import com.twitter.util.Await

class SampleAppIntegrationTest extends FeatureTest {

  override val app =
    new EmbeddedApp(
      new SampleGuiceApp,
      waitForWarmup = true,
      skipAppMain = true)

  "start app" in {
    app.start()
    app.appMain()

    Await.result(
      app.mainResult)
  }

  "throws `StartupTimeoutException` when app fails to start within `maxStartupTimeSeconds`" in {
    val guiceApp = new SampleGuiceApp {
      override protected def postWarmup(): Unit = {
        Thread.sleep(2000)
      }
    }
    val subject = new EmbeddedApp(
      guiceApp,
      waitForWarmup = true,
      skipAppMain = true,
      maxStartupTimeSeconds = 1)

    intercept[StartupTimeoutException] {
      subject.start()
    }
  }
}
