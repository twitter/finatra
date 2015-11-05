package com.twitter.inject.thrift

import com.twitter.scrooge.ThriftMethod

case class ThriftClientException(
  method: ThriftMethod,
  cause: Throwable)
  extends Exception(cause) {

  override def toString = {
    s"ThriftClientException($method, $cause)"
  }
}
