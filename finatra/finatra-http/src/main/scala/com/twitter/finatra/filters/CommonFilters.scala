package com.twitter.finatra.filters

import javax.inject.Inject

// TODO: replace ExceptionBarrierFilter (which is deprecated)
// with ExceptionMappingFilter and (finagle-http) StatsFilter
class CommonFilters @Inject()(
  a: AccessLoggingFilter,
  b: HttpResponseFilter,
  c: ExceptionBarrierFilter)
  extends MergedFilter(a, b, c)