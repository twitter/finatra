package com.twitter.inject.thrift

import com.twitter.inject.thrift.utils.ThriftMethodUtils._
import com.twitter.inject.utils.ExceptionUtils._
import com.twitter.scrooge.ThriftMethod

case class ThriftClientException(
  clientLabel: String,
  method: ThriftMethod,
  cause: Throwable)
  extends Exception(cause) {

  override def toString = {
    s"ThriftClientException: $clientLabel/${prettyStr(method)} = ${stripNewlines(cause)}"
  }
}
