package com.twitter.inject.thrift

import com.twitter.inject.WordSpecTest
import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.utils.ExceptionUtils
import com.twitter.scrooge.{ThriftStructCodec3, ThriftMethod}

class ThriftClientExceptionTest extends WordSpecTest {
  val FakeThriftMethod = new ThriftMethod {
    val name: String = "fakeThriftMethod"

    override def annotations: Map[String, String] = ???

    def serviceName: String = "FakeService"

    def serviceToFunction(svc: ServiceType): FunctionType = ???

    def oneway: Boolean = ???

    def argsCodec: ThriftStructCodec3[Args] = ???

    def responseCodec: ThriftStructCodec3[Result] = ???

    def functionToService(f: FunctionType): ServiceType = ???

    def toServiceIfaceService(f: FunctionType): ServiceIfaceServiceType = ???

    override def toString: String = name
  }


  "toString" in {
    val cause = new Exception("ThriftClientException")

    val thriftClientException = new ThriftClientException(
      "my-client",
      FakeThriftMethod,
      cause)

    thriftClientException.toString should equal(
      s"ThriftClientException: my-client/${ThriftMethodUtils.prettyStr(FakeThriftMethod)} = ${ExceptionUtils.stripNewlines(cause)}")
  }

}
