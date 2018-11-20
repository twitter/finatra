package com.twitter.finatra.thrift

import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.tracing.TraceId

/**
 * ThriftRequest was created to provide some metadata about a request. However
 * all of this data can be accessed elsewhere:
 *    methodName -> MethodMetadata.current.get.methodName
 *    traceId    -> Trace.id
 *    clientId   -> ClientId.current
 *
 * @see [[com.twitter.finagle.thrift.MethodMetadata]]
 * @see [[com.twitter.finagle.thrift.ClientId]]
 * @see [[com.twitter.finagle.tracing.TraceId]]
 */
@deprecated("Access the metadata contained in this class directly", "2018-11-10")
case class ThriftRequest[T](
  methodName: String,
  traceId: TraceId,
  clientId: Option[ClientId],
  args: T
)
