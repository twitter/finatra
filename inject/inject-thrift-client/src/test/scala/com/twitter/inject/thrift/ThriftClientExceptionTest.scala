package com.twitter.inject.thrift

import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.utils.ExceptionUtils
import com.twitter.inject.{Mockito, Test}
import com.twitter.scrooge.ThriftMethod

class ThriftClientExceptionTest extends Test with Mockito {
  private val FakeThriftMethod = smartMock[ThriftMethod]
  FakeThriftMethod.name returns "fakeThriftMethod"
  FakeThriftMethod.serviceName returns "FakeService"

  test("toString") {
    val cause = new Exception("ThriftClientException")
    val thriftClientException = ThriftClientException("my-client", FakeThriftMethod, cause)

    thriftClientException.toString should equal(
      s"ThriftClientException: my-client/${ThriftMethodUtils
        .prettyStr(FakeThriftMethod)} = ${ExceptionUtils.stripNewlines(cause)}"
    )
  }
}
