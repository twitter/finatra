package com.twitter.inject.thrift.integration.modules

import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.inject.Injector
import com.twitter.inject.thrift.modules.{DarkTrafficFilterModule, ReqRepDarkTrafficFilterModule}
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

@deprecated(
  "These tests exist to ensure legacy functionaly still operates. Do not use them for guidance",
  "2018-12-20")
class LegacyDoEverythingThriftServerDarkTrafficFilterModule
    extends DarkTrafficFilterModule[EchoService.ServiceIface] {

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
