package com.twitter.inject.thrift.integration.http_server

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.thrift.ThriftClientModule
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future
import com.twitter.conversions.time._

object EchoThriftClientModule extends ThriftClientModule[EchoService[Future]] {

  @Provides
  @Singleton
  def clientId: ClientId = ClientId("echo-http-service")

  override val label = "echo-service"
  override val dest = "flag!thrift-echo-service"
  override val connectTimeout = 1L.seconds
  override val requestTimeout = 1L.seconds
}
