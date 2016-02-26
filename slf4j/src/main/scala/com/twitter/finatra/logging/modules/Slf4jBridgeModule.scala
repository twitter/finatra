package com.twitter.finatra.logging.modules

import com.twitter.inject.{Injector, TwitterModule}
import org.slf4j.bridge.SLF4JBridgeHandler

object Slf4jBridgeModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    if (canInstallBridgeHandler) {
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()
    }
  }

  /* Private */

  private def canInstallBridgeHandler: Boolean = {
    // We do not want to attempt to install the bridge handler if the JDK14LoggerFactory
    // exists on the classpath. See: http://www.slf4j.org/legacy.html#jul-to-slf4j
    try {
      Class.forName("org.slf4j.impl.JDK14LoggerFactory", false, this.getClass.getClassLoader)
      false
    } catch {
      case e: ClassNotFoundException =>
        true
    }
  }
}
