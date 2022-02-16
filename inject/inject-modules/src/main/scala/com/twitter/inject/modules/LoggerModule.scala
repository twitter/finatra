package com.twitter.inject.modules

import com.twitter.inject.Injector
import com.twitter.inject.TwitterModule
import com.twitter.util

/**
 * A [[com.twitter.inject.TwitterModule]] which attempts to install the SLF4JBridgeHandler
 * when the `slf4j-jdk14` logging implementation is not present on the classpath.
 *
 * @see [[com.twitter.util.logging.Slf4jBridgeUtility]]
 */
@deprecated(
  "No replacement. Users should use the Slf4jBridgeUtility in the constructor of their application.",
  "2022-02-04")
object LoggerModule extends TwitterModule {

  override def singletonStartup(injector: Injector): Unit = {
    util.logging.Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation()
  }

  /**  Java-friendly way to access this module as a singleton instance */
  def get(): this.type = this
}
