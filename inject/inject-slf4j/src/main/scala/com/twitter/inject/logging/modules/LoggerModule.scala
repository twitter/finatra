package com.twitter.inject.logging.modules

import com.twitter.inject.logging.Slf4jBridgeUtility
import com.twitter.inject.{Injector, TwitterModule}

/**
 * A [[com.twitter.inject.TwitterModule]] which attempts to install the SLF4JBridgeHandler
 * when the `slf4j-jdk14` logging implementation is not present on the classpath.
 */
object LoggerModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation()
  }
}
