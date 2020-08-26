package com.twitter.inject.tests.thrift.utils

import com.twitter.inject.Test
import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.scrooge.ThriftMethodIface
import com.twitter.util.mock.Mockito

class ThriftMethodUtilsTest extends Test with Mockito {
  private val FakeThriftMethod = mock[ThriftMethodIface]
  FakeThriftMethod.name returns "Foo"
  FakeThriftMethod.serviceName returns "FooService"

  test("ThriftMethodUtils#return pretty string") {
    val prettyString = ThriftMethodUtils.prettyStr(FakeThriftMethod)
    prettyString should be("FooService.Foo")
  }
}
