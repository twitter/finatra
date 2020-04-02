package com.twitter.inject.tests.thrift.utils

import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.{Mockito, Test}
import com.twitter.scrooge.ThriftMethodIface

class ThriftMethodUtilsTest extends Test with Mockito {
  private val FakeThriftMethod = smartMock[ThriftMethodIface]
  FakeThriftMethod.name returns "Foo"
  FakeThriftMethod.serviceName returns "FooService"

  test("ThriftMethodUtils#return pretty string") {
    val prettyString = ThriftMethodUtils.prettyStr(FakeThriftMethod)
    prettyString should be("FooService.Foo")
  }
}
