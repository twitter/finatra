package com.twitter.inject.thrift.integration.integration2

import com.twitter.inject.IntegrationTest
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.thrift.{ThriftClientIdModule, ThriftClientModule}
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future
import javax.inject.Inject

class ThriftClientModuleNonMuxTest extends IntegrationTest {

  @Inject
  var echoService: EchoService[Future] = _

  override val injector = TestInjector(
    modules = Seq(ThriftClientModuleNonMux, ThriftClientIdModule, StatsReceiverModule),
    clientFlags = Map("com.twitter.server.resolverMap" -> "thrift-echo-service=nil!"))

  "test" in {
    assert(echoService != null)
  }

  object ThriftClientModuleNonMux extends ThriftClientModule[EchoService[Future]] {
    override val label = "echo-service"
    override val dest = "flag!thrift-echo-service"
    override val mux = false
  }
}


