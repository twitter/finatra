package com.twitter.finatra

import com.twitter.finagle.httpx.Request

package object filters {
  @deprecated("Use com.twitter.finatra.http.filters.AccessLoggingFilter", "")
  type AccessLoggingFilter = com.twitter.finatra.http.filters.AccessLoggingFilter[Request]

  @deprecated("Use com.twitter.finatra.http.filters.CommonFilters", "")
  type CommonFilters = com.twitter.finatra.http.filters.CommonFilters

  @deprecated("Use com.twitter.finatra.http.filters.ExceptionBarrierFilter", "")
  type ExceptionBarrierFilter = com.twitter.finatra.http.filters.ExceptionBarrierFilter

  @deprecated("Use com.twitter.finatra.http.filters.ExceptionMappingFilter", "")
  type ExceptionMappingFilter = com.twitter.finatra.http.filters.ExceptionMappingFilter[Request]

  @deprecated("Use com.twitter.finatra.http.filters.HttpResponseFilter", "")
  type HttpResponseFilter = com.twitter.finatra.http.filters.HttpResponseFilter[Request]
}
