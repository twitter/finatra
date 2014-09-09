package com.twitter.finatra.filters

import com.twitter.finatra.filters.general.{ExceptionBarrierFilter, HttpResponseFilter}
import com.twitter.finatra.filters.logging.AccessLoggingFilter
import javax.inject.Inject

class CommonFilters @Inject()(
  a: AccessLoggingFilter,
  b: HttpResponseFilter,
  c: ExceptionBarrierFilter)
  extends MergedFilter(a, b, c)