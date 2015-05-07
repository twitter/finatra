package com.twitter.inject.thrift.integration.http_server

import com.twitter.inject.thrift.{ThriftClientFilters, ThriftClientModule}
import com.twitter.inject.thrift.internal.ThriftClientExceptionFilter
import com.twitter.test.thriftscala.EchoService
import com.twitter.test.thriftscala.EchoService.{echo$result, setTimesToEcho$result}
import com.twitter.util.Future

object EchoThriftClientModule extends ThriftClientModule[EchoService[Future]] {
  override val label = "echo-service"
  override val clientId = "echo-http-service"
  override val dest = "flag!thrift-echo-service"
  override val connectTimeout = 1.second.toDuration
  override val requestTimeout = 1.second.toDuration

  override def configureFilters(clientFilters: ThriftClientFilters) = {
    clientFilters
      .filter[ThriftClientExceptionFilter]
      .globalTimeoutFilter(4.seconds)
      .exponentialRetryingFilter(
        start = 10.millis,
        multiplier = 2,
        numRetries = 4,
        NonFatalExceptions)
      .methodResultToFailureFilter[String](
        methodResult = echo$result,
        isFailure = _ == "woops")
      .filterMethod[SetTimesToEchoFailureFilter](
        methodResult = setTimesToEcho$result)
  }
}
