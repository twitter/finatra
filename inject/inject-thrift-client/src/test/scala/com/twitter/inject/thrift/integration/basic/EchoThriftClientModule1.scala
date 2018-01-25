package com.twitter.inject.thrift.integration.basic

import com.twitter.finagle.ThriftMux
import com.twitter.inject.thrift.modules.{PossiblyRetryableExceptions, ThriftClientModule}
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future

object EchoThriftClientModule1 extends ThriftClientModule[EchoService[Future]] {
  override val label = "echo-service"
  override val dest = "flag!thrift-echo-service"

  override def configureThriftMuxClient(
    client: ThriftMux.Client
  ): ThriftMux.Client = {
    client
      .withResponseClassifier(PossiblyRetryableExceptions)
  }
}
