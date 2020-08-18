package com.twitter.inject.tests.thrift.utils

import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.Test
import com.twitter.mock.Mockito
import com.twitter.scrooge.ThriftMethodIface

class ThriftMethodUtilsTest extends Test with Mockito {
  private val FakeThriftMethod = mock[ThriftMethodIface]
  FakeThriftMethod.name returns "Foo"
  FakeThriftMethod.serviceName returns "FooService"

  test("ThriftMethodUtils#return pretty string") {
    val prettyString = ThriftMethodUtils.prettyStr(FakeThriftMethod)
    prettyString should be("FooService.Foo")
  }
}
