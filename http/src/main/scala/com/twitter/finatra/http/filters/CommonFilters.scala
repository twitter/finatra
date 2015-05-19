package com.twitter.finatra.http.filters

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.filter.StatsFilter
import com.twitter.finatra.filters.MergedFilter
import javax.inject.Inject

class CommonFilters @Inject()(
  a: AccessLoggingFilter[Request],
  b: HttpResponseFilter[Request],
  c: StatsFilter[Request],
  d: ExceptionMappingFilter[Request])
  extends MergedFilter(a, b, c, d)
