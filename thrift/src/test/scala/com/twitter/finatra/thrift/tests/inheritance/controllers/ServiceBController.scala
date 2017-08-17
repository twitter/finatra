package com.twitter.finatra.thrift.tests.inheritance.controllers

import com.twitter.finatra.thrift.Controller
import com.twitter.serviceA.thriftscala.ServiceA
import com.twitter.serviceB.thriftscala.ServiceB
import com.twitter.util.Future

class ServiceBController extends Controller with ServiceB.BaseServiceIface {

  val ping = handle(ServiceB.Ping) { args: ServiceB.Ping.Args =>
    Future.value("pong")
  }

  val echo = handle(ServiceA.Echo) { args: ServiceA.Echo.Args =>
    Future.value(args.msg)
  }
}
