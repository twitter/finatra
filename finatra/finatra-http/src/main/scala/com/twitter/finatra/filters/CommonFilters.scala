package com.twitter.finatra.filters

import javax.inject.Inject

class CommonFilters @Inject()(
  a: AccessLoggingFilter,
  b: HttpResponseFilter,
  c: ExceptionBarrierFilter)
  extends MergedFilter(a, b, c)