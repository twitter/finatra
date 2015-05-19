package com.twitter.inject.thrift

import scala.reflect.ClassTag

@deprecated("Use com.twitter.inject.thrift.ThriftClientModule", "")
abstract class DeprecatedThriftClientModule[T: ClassTag]
  extends ThriftClientModule[T]