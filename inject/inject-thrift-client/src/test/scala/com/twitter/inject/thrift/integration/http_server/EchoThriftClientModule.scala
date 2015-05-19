package com.twitter.inject.thrift.integration.http_server

import com.twitter.inject.thrift.ThriftClientModule
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future
import com.twitter.conversions.time._

object EchoThriftClientModule extends ThriftClientModule[EchoService[Future]] {
  override val label = "echo-service"
  override val clientId = "echo-http-service"
  override val dest = "flag!thrift-echo-service"
  override val connectTimeout = 1L.seconds
  override val requestTimeout = 1L.seconds
}
