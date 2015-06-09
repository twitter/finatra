package com.twitter.inject

/**
 * Guice/twitter.App lifecycle integrations
 */
trait TwitterModuleLifecycle {

  /* Protected */

  /*
   * Protected Lifecycle
   * TODO: Consider eliminating the following lifecycle methods by more generally supporting @PostConstruct, @PreDestroy, and @Warmup (see Onami-Lifecycle or Governator for examples)
   */

  /**
   * Invoke after Guice injector is started
   * NOTE: This method should only get singleton instances from the injector.
   */
  protected[inject] def singletonStartup(injector: Injector) {}

  /**
   * Invoke as JVM shuts down.
   * NOTE: This method should only get singleton instances from the injector.
   */
  protected[inject] def singletonShutdown(injector: Injector) {}
}
