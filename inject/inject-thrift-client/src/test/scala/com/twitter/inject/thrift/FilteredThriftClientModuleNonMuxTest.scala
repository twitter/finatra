package com.twitter.inject.thrift

import com.twitter.greeter.thriftscala.Greeter
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.thrift.filters.ThriftClientFilterBuilder
import com.twitter.inject.thrift.modules.{FilteredThriftClientModule, ThriftClientIdModule}
import com.twitter.inject.{Injector, IntegrationTest, TypeUtils}
import com.twitter.util.Future

class FilteredThriftClientModuleNonMuxTest extends IntegrationTest {

  override val injector: Injector = TestInjector(
    modules = Seq(
      FilteredThriftClientModuleNonMux,
      ThriftClientIdModule,
      StatsReceiverModule
    ),
    flags = Map("com.twitter.server.resolverMap" -> "greeter-thrift-service=nil!")
  ).create

  lazy val greeter: Greeter[Future] =
    injector.instance[Greeter[Future]](TypeUtils.asManifest[Greeter[Future]])

  test("bind expected type properly") {
    assert(greeter != null)
  }

  object FilteredThriftClientModuleNonMux
    extends FilteredThriftClientModule[Greeter[Future], Greeter.ServiceIface] {
    override val label = "greeter-thrift-client"
    override val dest = "flag!greeter-thrift-service"
    override val mux = false

    override def filterServiceIface(
      serviceIface: Greeter.ServiceIface,
      filter: ThriftClientFilterBuilder
    ): Greeter.ServiceIface = {
      serviceIface
    }
  }

}
