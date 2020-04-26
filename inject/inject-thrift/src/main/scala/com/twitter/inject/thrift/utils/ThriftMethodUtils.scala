package com.twitter.inject.thrift.utils

import com.twitter.scrooge.ThriftMethodIface
import com.twitter.util.Memoize

object ThriftMethodUtils {

  val prettyStr = Memoize { method: ThriftMethodIface =>
    method.serviceName + "." + method.name
  }
}
