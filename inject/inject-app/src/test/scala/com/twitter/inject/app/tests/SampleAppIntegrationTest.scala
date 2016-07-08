package com.twitter.inject.app.tests

import com.twitter.inject.Test
import com.twitter.inject.app.EmbeddedApp

class SampleAppIntegrationTest extends Test {

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
