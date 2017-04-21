package com.twitter.inject.logging

import com.twitter.inject.Logging
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

@deprecated("Use com.twitter.util.logging.Slf4jBridgeUtility in util-slf4j-jul-bridge.", "2017-03-06")
object Slf4jBridgeUtility extends Logging {

  private[inject] def attemptSlf4jBridgeHandlerInstallation(): Unit = {
    if (!SLF4JBridgeHandler.isInstalled && canInstallBridgeHandler) {
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()
      info("org.slf4j.bridge.SLF4JBridgeHandler installed.")
    }
  }

  /* Private */

  private def canInstallBridgeHandler: Boolean = {
    // We do not want to attempt to install the bridge handler if the JDK14LoggerFactory
    // exists on the classpath. See: http://www.slf4j.org/legacy.html#jul-to-slf4j
    try {
      Class.forName("org.slf4j.impl.JDK14LoggerFactory", false, this.getClass.getClassLoader)
      LoggerFactory.getLogger(this.getClass).warn("Detected [org.slf4j.impl.JDK14LoggerFactory] on classpath. SLF4JBridgeHandler cannot be installed, see: http://www.slf4j.org/legacy.html#jul-to-slf4j")
      false
    } catch {
      case e: ClassNotFoundException =>
        true
    }
  }
}
