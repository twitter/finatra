package com.twitter.inject.thrift

import com.twitter.inject.thrift.conversions.method._
import com.twitter.inject.utils.ExceptionUtils._
import com.twitter.scrooge.ThriftMethod

case class ThriftClientException(
  method: ThriftMethod,
  cause: Throwable)
  extends Exception(cause) {

  override def toString = {
    s"ThriftClientException: ${method.toPrettyString} = ${stripNewlines(cause)}"
  }
}
