package com.twitter.inject.app.tests

import com.twitter.inject.app.{EmbeddedApp, FeatureTest}
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
}
