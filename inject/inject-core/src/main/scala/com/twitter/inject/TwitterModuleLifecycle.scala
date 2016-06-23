package com.twitter.inject

/**
 * [[com.twitter.inject.TwitterModule]] to com.twitter.inject.app.App lifecycle integration
 */
trait TwitterModuleLifecycle {

  /* Protected */

  /**
   * Invoke after the injector is started
   *
   * This method should only get singleton instances from the injector.
   */
  protected[inject] def singletonStartup(injector: Injector): Unit = {}

  /**
  * Invoke after external ports are bound and any clients are resolved
  *
  * This method should only get singleton instances from the injector.
  */
  protected[inject] def singletonPostWarmupComplete(injector: Injector): Unit = {}

  /**
   * Invoke as JVM shuts down.
   *
   * This method should only get singleton instances from the injector.
   */
  protected[inject] def singletonShutdown(injector: Injector): Unit = {}
}
