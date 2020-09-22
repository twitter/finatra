package com.twitter.inject.thrift.integration.modules

import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.inject.Injector
import com.twitter.inject.thrift.modules.ReqRepDarkTrafficFilterModule
import com.twitter.test.thriftscala.EchoService

object DoEverythingThriftServerDarkTrafficFilterModule
    extends ReqRepDarkTrafficFilterModule[EchoService.ReqRepServicePerEndpoint] {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   */
  override def enableSampling(injector: Injector): Any => Boolean = { _ =>
    MethodMetadata.current match {
      case Some(m) => !(m.methodName.equals("setTimesToEcho"))
      case _ => true
    }
  }
}
