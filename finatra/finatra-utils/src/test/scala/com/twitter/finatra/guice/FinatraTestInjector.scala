package com.twitter.finatra.guice

import com.google.inject.Stage
import com.twitter.inject.{Injector, TwitterModule}

@deprecated("Use com.twitter.inject.app.TestInjector", "")
object FinatraTestInjector {
  def apply(modules: TwitterModule*): Injector = {
    com.twitter.inject.app.TestInjector.apply(modules = modules)
  }
}

@deprecated("Use com.twitter.inject.app.TestInjector", "")
class FinatraTestInjector {

  def apply(modules: TwitterModule*): Injector = {
    com.twitter.inject.app.TestInjector.apply(modules = modules)
  }

  def apply(
    clientFlags: Map[String, String] = Map(),
    modules: Seq[TwitterModule],
    overrideModules: Seq[TwitterModule] = Seq(),
    stage: Stage = Stage.DEVELOPMENT): Injector = {

    com.twitter.inject.app.TestInjector.apply(
      clientFlags,
      modules,
      overrideModules,
      stage)
  }
}