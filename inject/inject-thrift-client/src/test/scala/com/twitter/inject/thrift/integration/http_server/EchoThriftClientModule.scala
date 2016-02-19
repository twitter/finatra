package com.twitter.inject.thrift.integration.http_server

import com.twitter.inject.thrift.modules.ThriftClientModule
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future

object EchoThriftClientModule extends ThriftClientModule[EchoService[Future]] {
  override val label = "echo-service"
  override val dest = "flag!thrift-echo-service"
}
