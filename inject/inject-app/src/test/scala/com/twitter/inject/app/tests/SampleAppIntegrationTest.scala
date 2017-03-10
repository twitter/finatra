package com.twitter.inject.app.tests

import com.twitter.inject.Test
import com.twitter.inject.app.EmbeddedApp

class SampleAppIntegrationTest extends Test {

  test("start app") {
    val app =
      new EmbeddedApp(new SampleApp)

    app.main()
  }

  test("exception in App#run() throws") {
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
