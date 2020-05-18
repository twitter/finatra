package com.twitter.inject.logging

/** Keys for common MDC values */
object MDCKeys {

  // Tracing MDC keys
  val TraceId: String = "traceId"
  val TraceSampled: String = "traceSampled"
  val TraceSpanId: String = "traceSpanId"
}
