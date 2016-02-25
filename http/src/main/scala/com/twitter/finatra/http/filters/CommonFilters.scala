package com.twitter.finatra.http.filters

import com.twitter.finagle.http.Request
import com.twitter.finatra.filters.MergedFilter
import javax.inject.{Inject, Singleton}

@Singleton
class CommonFilters @Inject()(
  a: StatsFilter[Request],
  b: AccessLoggingFilter[Request],
  c: HttpResponseFilter[Request],
  d: ExceptionMappingFilter[Request])
  extends MergedFilter(a, b, c, d)
