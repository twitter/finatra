package com.twitter.inject

import scala.collection.mutable

/**
 * Guice/twitter.App lifecycle integrations
 */
trait TwitterModuleLifecycle extends Logging {

  /* Mutable State */
  private val postStartupFunctions = mutable.Buffer[Injector => Unit]()
  private val shutdownFunctions = mutable.Buffer[() => Unit]()

  /* Protected */

  /*
   * Protected Lifecycle
   * TODO: Consider eliminating the following lifecycle methods by more generally supporting @PostConstruct, @PreDestroy, and @Warmup (see Onami-Lifecycle or Governator for examples)
   */

  /**
   * Invoke `singleton func` after Guice injector is started
   * NOTE: This method should only be called from a @Singleton 'provides' method to avoid registering
   * multiple startup hooks every time an object is created.
   */
  protected def singletonStartup(func: Injector => Unit) {
    postStartupFunctions += func
  }

  /**
   * Invoke after Guice injector is started
   * NOTE: This method should only get singleton instances from the injector.
   */
  protected def singletonStartup(injector: Injector) {}

  /**
   * Invoke 'singleton func' as JVM shuts down.
   * NOTE: This method should only be called from a @Singleton 'provides' method to avoid registering
   * multiple shutdown hooks every time an object is created.
   */
  protected def singletonShutdown(func: => Unit) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() = func
    })
  }

  /**
   * Invoke as JVM shuts down.
   * NOTE: This method should only get singleton instances from the injector.
   */
  protected def singletonShutdown(injector: Injector) {}

  /* Private */

  private[inject] def callPostStartupCallbacks(injector: Injector) {
    if (postStartupFunctions.nonEmpty) {
      info("Calling PostStartup methods in " + this.getClass.getSimpleName)
    }
    postStartupFunctions foreach {_(injector)}
    singletonStartup(injector)
  }

  private[inject] def callShutdownCallbacks(injector: Injector) {
    if (shutdownFunctions.nonEmpty) {
      info("Calling Shutdown methods in " + this.getClass.getSimpleName)
    }
    shutdownFunctions foreach {_()}
    singletonShutdown(injector)
  }
}
