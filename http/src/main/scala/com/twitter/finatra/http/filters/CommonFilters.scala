package com.twitter.finatra.http.filters

import com.twitter.finagle.http.Request
import com.twitter.finatra.filters.MergedFilter
import com.twitter.finatra.http.filters.StatsFilter
import javax.inject.Inject

class CommonFilters @Inject()(
  a: StatsFilter[Request],
  b: AccessLoggingFilter[Request],
  c: HttpResponseFilter[Request],
  d: ExceptionMappingFilter[Request])
  extends MergedFilter(a, b, c, d)
