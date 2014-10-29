package com.twitter.finatra.integration.app

import com.twitter.finatra.guice.GuiceApp

object SampleGuiceAppMain extends SampleGuiceApp

class SampleGuiceApp extends GuiceApp {

  override val modules = Seq()

  override def appMain() {
    injector.instance[SampleManager].start()
  }
}
