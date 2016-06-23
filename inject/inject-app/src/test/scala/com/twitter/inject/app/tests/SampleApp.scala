package com.twitter.inject.app.tests

import com.twitter.inject.app.App

object SampleAppMain extends SampleApp

class SampleApp extends App {
  override val name = "sample-app"

  override val modules = Seq()

  override protected def run(): Unit = {
    injector.instance[SampleManager].start()
  }
}
