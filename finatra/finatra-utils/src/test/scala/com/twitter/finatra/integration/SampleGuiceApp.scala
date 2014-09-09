package com.twitter.finatra.integration

import com.twitter.finatra.guice.GuiceApp

class SampleGuiceApp extends GuiceApp {

  override val modules = Seq()

  override def appMain() {
    injector.instance[SampleManager].start()
  }
}
