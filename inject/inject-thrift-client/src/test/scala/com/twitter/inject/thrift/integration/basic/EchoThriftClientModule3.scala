package com.twitter.inject.thrift.integration.basic

import com.twitter.inject.thrift.modules.ThriftClientModule
import com.twitter.test.thriftscala.EchoService

object EchoThriftClientModule3 extends ThriftClientModule[EchoService.MethodPerEndpoint] {
  override val label = "echo-service"
  override val dest = "flag!thrift-echo-service"
}
