package com.twitter.finatra.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.internal.exceptions.ExceptionManager
import com.twitter.util.{Future, NonFatal}
import javax.inject.{Inject, Singleton}

/**
 * Filter which converts exceptions into HTTP responses.
 *
 * NOTE: Should be as close to the start of the filter chain as possible.
 *
 * TODO (AF-302): Are we handling Fatal errors properly?
 */
@Singleton
class ExceptionMappingFilter @Inject()(
  exceptionManager: ExceptionManager)
  extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]) = {
    (try service(request) catch {
      case e: NoSuchMethodException =>
        // Catch this instead of propagating it to the RootMonitor and then
        // closing the connection.
        // TODO (AF-302): for more comprehensive solution.
        Future.exception(e)
    }) handle { case e =>
      exceptionManager.toResponse(request, e)
    }
  }
}
