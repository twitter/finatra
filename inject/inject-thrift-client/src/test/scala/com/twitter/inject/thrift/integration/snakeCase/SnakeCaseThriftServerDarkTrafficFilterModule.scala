package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.inject.Injector
import com.twitter.inject.thrift.modules.ReqRepDarkTrafficFilterModule
import com.twitter.snakeCase.thriftscala.SnakeCaseService

object SnakeCaseThriftServerDarkTrafficFilterModule
    extends ReqRepDarkTrafficFilterModule[SnakeCaseService.ReqRepServicePerEndpoint] {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   */
  override def enableSampling(injector: Injector): Any => Boolean = { _ =>
    MethodMetadata.current match {
      case Some(m) =>
        m.methodName.equals("enqueue_event")
      case _ =>
        true
    }
  }
}
