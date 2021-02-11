package com.twitter.finatra.http.exceptions

import scala.util.control.NoStackTrace

/**
 * Used to denote an exception which occurred during routing while attempting to
 * extract the capture value of a route param from an incoming request URI, e.g.,
 * for a defined route: `/user/:id`, extracting the text "123" from an incoming
 * request URI of `/user/123`.
 *
 * This exception is handled by the framework but is public to allow users to customize
 * their server behavior via an installed [[ExceptionMapper]] over this exception type, if
 * desired. Note, that this exception occurs **before** routing and thus any user-defined
 * [[ExceptionMapper]] should be added to an [[ExceptionManager]] installed on an
 * [[com.twitter.finatra.http.filters.ExceptionMappingFilter]] that is installed with
 * `beforeRouting = true`.
 *
 * {{{
 *   class RouteParamExtractionExceptionMapper
 *     extends ExceptionMapper[RouteParamExtractionException] {
 *     def toResponse(request: Request, throwable: RouteParamExtractionException): Response  = ???
 *   }
 *
 *   ...
 *
 *   val beforeRoutingExceptionManager: ExceptionManager =
 *     new ExceptionManager(injector, injector.instance[StatsReceiver])
 *
 *   beforeRoutingExceptionManager.add[RouteParamExtractionExceptionMapper]
 *
 *   ...
 *
 *   override def configureHttp(router: HttpRouter) {
 *     router
 *       .filter(new ExceptionMappingFilter[Request](beforeRoutingExceptionManager), beforeRouting = true)
 *       .add[MyAPIController]
 *   }
 *
 *   // or without a custom `ExceptionManager` (uses the default configured by the `ExceptionManagerModule`):
 *
 *   override def configureHttp(router: HttpRouter) {
 *    router
 *      .filter[ExceptionMappingFilter[Request]](beforeRouting = true)
 *      .add[MyAPIController]
 *      .exceptionMapper[RouteParamExtractionExceptionMapper]
 *    }
 * }}}
 *
 * @param cause the underlying cause of this exception.
 */
class RouteParamExtractionException private[finatra] (cause: Throwable)
    extends Exception(cause)
    with NoStackTrace
