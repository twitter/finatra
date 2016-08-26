package com.twitter.finatra.thrift.tests.noninjection

import com.twitter.finatra.thrift.Controller
import com.twitter.noninjection.thriftscala.NonInjectionService
import com.twitter.noninjection.thriftscala.NonInjectionService.Echo
import com.twitter.util.Future

class NonInjectionController()
  extends Controller
  with NonInjectionService.BaseServiceIface {

  override val echo = handle(Echo) { args: Echo.Args =>
    Future.value(args.msg)
  }

}
