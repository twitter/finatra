package com.twitter.finatra.thrift.internal.routing

import com.twitter.finagle.Service
import com.twitter.finagle.thrift.ThriftService

private[thrift] case class Services(service: Service[Array[Byte], Array[Byte]], thriftService: ThriftService)
