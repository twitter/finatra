package com.twitter.inject.tests.thrift.utils

import com.twitter.inject.WordSpecTest
import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.scrooge.{ThriftStructCodec3, ThriftMethod}

class ThriftMethodUtilsTest extends WordSpecTest {

  "ThriftMethodUtils" should {

    "return pretty string" in {

      val method = new ThriftMethod {
        val name: String = "Foo"

        override def annotations: Map[String, String] = ???

        def toServiceIfaceService(f: FunctionType): ServiceIfaceServiceType = ???

        def functionToService(f: FunctionType): ServiceType = ???

        def serviceToFunction(svc: ServiceType): FunctionType = ???

        val serviceName: String = "FooService"

        def argsCodec: ThriftStructCodec3[Args] = ???

        def responseCodec: ThriftStructCodec3[Result] = ???

        val oneway: Boolean = false
      }

      val prettyString = ThriftMethodUtils.prettyStr(method)
      prettyString should be("FooService.Foo")
    }
  }
}
