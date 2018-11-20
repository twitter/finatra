package com.twitter.finatra.thrift.tests.doeverything.modules

import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.finatra.thrift.modules.DarkTrafficFilterModule
import com.twitter.inject.Injector

class DoEverythingThriftServerDarkTrafficFilterModule
    extends DarkTrafficFilterModule[DoEverything.ServiceIface] {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   */
  override def enableSampling(injector: Injector): Any => Boolean = { request =>

    MethodMetadata.current match {
      case Some(m) => !(m.methodName.equals("uppercase") || m.methodName.equals("moreThanTwentyTwoArgs"))
      case _ => true
    }
  }
}
