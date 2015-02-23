package com.twitter.finatra.logging.modules

import com.twitter.finatra.guice.GuiceModule
import org.slf4j.bridge.SLF4JBridgeHandler

object LogbackModule extends GuiceModule {
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()
}
