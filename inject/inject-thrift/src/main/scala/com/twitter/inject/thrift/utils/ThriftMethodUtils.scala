package com.twitter.inject.thrift.utils

import com.twitter.scrooge.ThriftMethod
import com.twitter.util.Memoize

object ThriftMethodUtils {

  val prettyStr = Memoize { method: ThriftMethod =>
    method.serviceName + "." + method.name
  }
}
