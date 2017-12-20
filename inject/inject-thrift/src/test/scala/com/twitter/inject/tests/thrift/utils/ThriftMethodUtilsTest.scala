package com.twitter.inject.tests.thrift.utils

import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.{Mockito, Test}
import com.twitter.scrooge.ThriftMethod

class ThriftMethodUtilsTest extends Test with Mockito {
  private val FakeThriftMethod = smartMock[ThriftMethod]
  FakeThriftMethod.name returns "Foo"
  FakeThriftMethod.serviceName returns "FooService"

  test("ThriftMethodUtils#return pretty string") {
    val prettyString = ThriftMethodUtils.prettyStr(FakeThriftMethod)
    prettyString should be("FooService.Foo")
  }
}
