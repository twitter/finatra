package com.twitter.inject.thrift.internal

import com.twitter.greeter.thriftscala.Greeter
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.thrift.modules.{ThriftClientModule, ThriftClientIdModule}

class ThriftClientModuleNonMuxTest extends Test {

  val injector = TestInjector(
    modules = Seq(ThriftClientModuleNonMux, ThriftClientIdModule, StatsReceiverModule),
    flags = Map("com.twitter.server.resolverMap" -> "greeter-thrift-service=nil!"))

  "test" in {
    val client = injector.instance[Greeter.FutureIface]
    assert(client != null)
  }

  object ThriftClientModuleNonMux extends ThriftClientModule[Greeter.FutureIface] {
    override val label = "greeter-thrift-client"
    override val dest = "flag!greeter-thrift-service"
    override val mux = false
  }
}
