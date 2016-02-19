package com.twitter.inject

package object thrift {

  // @deprecated("Use com.twitter.inject.thrift.modules.ThriftClientIdModule", "2016-02-11")
  object ThriftClientIdModule extends com.twitter.inject.thrift.modules.ThriftClientIdModule

  // @deprecated("Use com.twitter.inject.thrift.modules.ThriftClientModule", "2016-02-11")
  type ThriftClientModule[T] = com.twitter.inject.thrift.modules.ThriftClientModule[T]

}
