package com.twitter.finatra.thrift.tests.inheritance

import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.inheritance.controllers.ServiceBController

object InheritanceServerMain extends InheritanceServer

class InheritanceServer extends ThriftServer {
  override val name = "inherited-server"

  protected def configureThrift(router: ThriftRouter) = {
    router
      .add[ServiceBController]
  }
}
