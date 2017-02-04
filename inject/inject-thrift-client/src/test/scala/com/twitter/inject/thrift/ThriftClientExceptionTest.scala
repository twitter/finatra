package com.twitter.inject.thrift

import com.twitter.inject.WordSpecTest
import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.utils.ExceptionUtils
import com.twitter.scrooge.{ThriftStructCodec3, ThriftMethod}

class ThriftClientExceptionTest extends WordSpecTest {
  val FakeThriftMethod = new ThriftMethod {
    override val name = "fakeThriftMethod"

    /** Thrift service name. A thrift service is a list of methods. */
    override def serviceName: String = "FakeService"

    /** Convert a service implementation of this method into a function implementation */
    override def serviceToFunction(svc: ServiceType): FunctionType = ???

    /** True for oneway thrift methods */
    override def oneway: Boolean = ???

    /** Codec for the request args */
    override def argsCodec: ThriftStructCodec3[Args] = ???

    /** Codec for the response */
    override def responseCodec: ThriftStructCodec3[Result] = ???

    /** Convert a function implementation of this method into a service implementation */
    override def functionToService(f: FunctionType): ServiceType = ???

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
