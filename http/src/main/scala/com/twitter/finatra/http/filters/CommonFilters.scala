package com.twitter.finatra.http.filters

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.filter.StatsFilter
import com.twitter.finatra.filters.MergedFilter
import javax.inject.Inject

class CommonFilters @Inject()(
  a: AccessLoggingFilter,
  b: HttpResponseFilter,
  c: StatsFilter[Request],
  d: ExceptionMappingFilter)
  extends MergedFilter(a, b, c, d)
