package com.twitter.inject.app.tests.internal

import com.google.inject.name.Names
import com.twitter.inject.{Injector, TwitterModule}

class TestModuleClass(instance: String, var counter: Int) extends TwitterModule {
  override protected def configure(): Unit = {
    info(s"Binding String to instance value: $instance with @Name($instance) annotation")
    bind[String].annotatedWith(Names.named(instance)).toInstance(instance)
  }

  /**
   * Invoked after the injector is started.
   *
   * This method should only get singleton instances from the injector.
   */
  override protected[inject] def singletonStartup(injector: Injector): Unit = {
    counter = counter + 1
  }
}
