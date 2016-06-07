package com.twitter.inject.thrift.conversions

import com.twitter.scrooge.ThriftMethod

object method {

  implicit class RichThriftMethod(val self: ThriftMethod) extends AnyVal {
    def toPrettyString: String = {
      self.serviceName + "." + self.name
    }
  }
}
