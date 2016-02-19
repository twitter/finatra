package com.twitter.inject.thrift.conversions

import com.twitter.scrooge.ThriftMethod

object method {

  implicit class RichThriftMethod(val wrapped: ThriftMethod) {
    def toPrettyString: String = {
      wrapped.serviceName + "." + wrapped.name
    }
  }
}
