package com.twitter.finatra.thrift.tests.noninjection

import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter

object NonInjectionThriftServerMain extends NonInjectionThriftServer

class NonInjectionThriftServer extends ThriftServer {
  override val name = "noninjection-example-server"

  override def configureThrift(router: ThriftRouter): Unit = {
    router.add(new NonInjectionController())
  }
}
