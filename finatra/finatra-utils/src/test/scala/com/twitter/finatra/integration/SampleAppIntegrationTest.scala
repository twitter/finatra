package com.twitter.finatra.integration

import com.twitter.finatra.integration.app.SampleGuiceApp
import com.twitter.finatra.test.{EmbeddedApp, IntegrationTest}
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
