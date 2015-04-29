package com.twitter.inject.app.tests

import com.twitter.inject.app.{EmbeddedApp, IntegrationTest}
import com.twitter.util.Await

class SampleAppIntegrationTest extends IntegrationTest {

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
}
