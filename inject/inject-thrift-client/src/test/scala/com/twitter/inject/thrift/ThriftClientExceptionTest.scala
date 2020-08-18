package com.twitter.inject.thrift

import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.utils.ExceptionUtils
import com.twitter.inject.Test
import com.twitter.mock.Mockito
import com.twitter.scrooge.ThriftMethodIface

class ThriftClientExceptionTest extends Test with Mockito {
  private val FakeThriftMethod = mock[ThriftMethodIface]
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
