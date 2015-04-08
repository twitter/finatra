package com.twitter.inject.thrift

import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import com.twitter.test.thriftscala.EchoService

class ThriftClientModuleTest extends Test {

  /*
   * NOTE: This test uses EchoService.FutureIface instead of the preferred EchoService[Future] due
   * to higher kinded types not playing well with manifests (which we need to use to call injector.instance[EchoService[Future]].
   * In production code, the EchoService[Future] is constructor injected which avoids these issues.
   */
  "ThriftClientModule" should {
    "create mux clients" in {
      val module = new ThriftClientModule[EchoService.FutureIface] {
        override val label = "echo-service"
        override val dest = "/s/echo/echo"
      }

      assertInjection(module)
    }
    "create non mux clients" in {
      val module = new ThriftClientModule[EchoService.FutureIface] {
        override val label = "echo-service"
        override val dest = "/s/echo/echo"
        override val mux = false
      }

      assertInjection(module)
    }
  }

  private def assertInjection(module: ThriftClientModule[EchoService.FutureIface]) {
    val injector = TestInjector(module)
    val client = injector.instance[EchoService.FutureIface]
    assert(client != null)
  }
}
