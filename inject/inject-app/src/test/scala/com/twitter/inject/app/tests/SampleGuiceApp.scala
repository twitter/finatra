package com.twitter.inject.app.tests

import com.twitter.inject.app.App

object SampleGuiceAppMain extends SampleGuiceApp

class SampleGuiceApp extends App {

  override val modules = Seq()

  override def appMain() {
    injector.instance[SampleManager].start()
  }
}
