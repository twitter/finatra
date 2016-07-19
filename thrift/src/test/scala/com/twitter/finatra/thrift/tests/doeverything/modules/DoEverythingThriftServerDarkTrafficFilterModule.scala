package com.twitter.finatra.thrift.tests.doeverything.modules

import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.finatra.thrift.ThriftRequest
import com.twitter.finatra.thrift.modules.DarkTrafficFilterModule

class DoEverythingThriftServerDarkTrafficFilterModule
  extends DarkTrafficFilterModule[DoEverything.ServiceIface]{

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   */
  override val enableSampling: ThriftRequest[_] => Boolean = { request =>
    request.methodName match {
      case "uppercase" => false // used in warmup
      case "moreThanTwentyTwoArgs" => false
      case _ => true
    }
  }

}
