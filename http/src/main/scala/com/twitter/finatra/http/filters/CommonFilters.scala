package com.twitter.finatra.http.filters

import com.twitter.finagle.http.Request
import com.twitter.finatra.filters.MergedFilter
import javax.inject.{Inject, Singleton}

/**
 * A typical collection of filters for a Finatra Http service. Ordering of
 * filters is important.
 *
 * HttpNackFilter converts Finagle's nacks into HttpExceptions. This must come
 * below the ExceptionMappingFilter or else that will convert them to generic
 * 500s.
 */
@Singleton
class CommonFilters @Inject()(
  a: StatsFilter[Request],
  b: AccessLoggingFilter[Request],
  c: HttpResponseFilter[Request],
  d: ExceptionMappingFilter[Request],
  e: HttpNackFilter[Request])
  extends MergedFilter(a, b, c, d, e)