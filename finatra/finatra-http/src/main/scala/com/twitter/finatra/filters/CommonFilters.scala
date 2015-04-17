package com.twitter.finatra.filters

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.filter.StatsFilter
import javax.inject.Inject

class CommonFilters @Inject()(
  a: AccessLoggingFilter,
  b: HttpResponseFilter,
  c: StatsFilter[Request],
  d: ExceptionMappingFilter)
  extends MergedFilter(a, b, c, d)
