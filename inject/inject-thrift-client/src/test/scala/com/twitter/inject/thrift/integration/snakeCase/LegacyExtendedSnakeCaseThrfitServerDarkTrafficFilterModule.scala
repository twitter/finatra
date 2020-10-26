package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.inject.Injector
import com.twitter.inject.thrift.modules.DarkTrafficFilterModule
import com.twitter.snakeCase.thriftscala.ExtendedSnakeCaseService

class LegacyExtendedSnakeCaseThrfitServerDarkTrafficFilterModule
    extends DarkTrafficFilterModule[ExtendedSnakeCaseService.ServiceIface] {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   */
  override def enableSampling(injector: Injector): Any => Boolean = { _ =>
    MethodMetadata.current match {
      case Some(m) =>
        // Test one inherited and one declared method
        m.methodName.equals("enqueue_event") || m.methodName.equals("additional_event")
      case _ =>
        true
    }
  }
}
