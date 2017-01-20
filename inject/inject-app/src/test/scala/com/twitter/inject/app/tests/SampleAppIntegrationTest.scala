package com.twitter.inject.app.tests

import com.twitter.inject.WordSpecTest
import com.twitter.inject.app.EmbeddedApp

class SampleAppIntegrationTest extends WordSpecTest {

  "start app" in {
    val app =
      new EmbeddedApp(new SampleApp)

    app.main()
  }

  "exception in App#run() throws" in {
    val app = new EmbeddedApp(
      new SampleApp {
        override protected def run(): Unit = {
          throw new RuntimeException("FORCED EXCEPTION")
        }})

    intercept[Exception] {
      app.main()
    }
  }
}
