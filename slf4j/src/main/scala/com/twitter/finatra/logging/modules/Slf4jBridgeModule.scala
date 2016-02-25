package com.twitter.finatra.logging.modules

import com.twitter.inject.{Injector, TwitterModule}
import org.slf4j.bridge.SLF4JBridgeHandler

private[finatra] object Slf4jBridgeModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
  }
}
