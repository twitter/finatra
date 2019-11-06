package com.twitter.inject.app.tests

import com.google.inject.name.Names
import com.twitter.inject.{Injector, TwitterModule}

object TestModuleObject extends TwitterModule {
  override protected def configure(): Unit = {
    bind[String].annotatedWith(Names.named("object")).toInstance("object")
  }

  /**
   * Invoked after the injector is started.
   *
   * This method should only get singleton instances from the injector.
   */
  override protected[inject] def singletonStartup(injector: Injector): Unit = {
    val state = injector.instance[StateMap]
    val count = state.internals.getOrElseUpdate("key", 0)
    state.internals.+=(("key", count + 1))
  }
}
