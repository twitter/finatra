package com.twitter.inject.modules

import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util

/**
 * A [[com.twitter.inject.TwitterModule]] which attempts to install the SLF4JBridgeHandler
 * when the `slf4j-jdk14` logging implementation is not present on the classpath.
 *
 * @see [[com.twitter.util.logging.Slf4jBridgeUtility]]
 */
object LoggerModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    util.logging.Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation()
  }
}
