package com.twitter.inject.thrift

import com.twitter.util.NoStacktrace

/**
 * Wraps exceptions originating from a thrift client call
 */
case class ThriftClientException(
  serviceName: String,
  methodName: String,
  exception: Throwable)
  extends Exception(s"$serviceName#$methodName failed with $exception", exception)
  with NoStacktrace