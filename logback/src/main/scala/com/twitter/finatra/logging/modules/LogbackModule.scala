package com.twitter.finatra.logging.modules

import com.twitter.inject.TwitterModule
import org.slf4j.bridge.SLF4JBridgeHandler

object LogbackModule extends TwitterModule {

  singletonStartup { _ =>
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
  }
}
