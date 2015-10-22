package com.twitter.finatra.thrift

import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.tracing.TraceId

case class ThriftRequest(
  methodName: String,
  traceId: TraceId,
  clientId: Option[ClientId],
  args: Any)