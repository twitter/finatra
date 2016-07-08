package com.twitter.finatra.http.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.exceptions.ExceptionManager
import javax.inject.{Inject, Singleton}

/**
 * Filter which converts exceptions into HTTP responses.
 * NOTE: Should be as close to the start of the filter chain as possible.
 */
@Singleton
class ExceptionMappingFilter[R <: Request] @Inject()(
  exceptionManager: ExceptionManager)
  extends SimpleFilter[R, Response] {

  override def apply(request: R, service: Service[R, Response]) = {
    service(request).handle { case e =>
      exceptionManager.toResponse(request, e)
    }
  }
}
