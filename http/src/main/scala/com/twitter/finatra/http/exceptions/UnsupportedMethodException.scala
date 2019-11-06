package com.twitter.finatra.http.exceptions

import scala.util.control.NoStackTrace

/**
 * Used to denote that an incoming request HTTP method (verb) is not supported by a
 * matched route (that is a route exists which matches the incoming request URI but
 * is not defined over the incoming request method).
 *
 * This exception is handled by the framework but is public to allow users to customize
 * their server behavior via an installed [[ExceptionMapper]] over this exception type, if
 * desired. Note, that this exception occurs **before** routing and thus any user-defined
 * [[ExceptionMapper]] should be added to an [[ExceptionManager]] installed on an
 * [[com.twitter.finatra.http.filters.ExceptionMappingFilter]] that is installed with
 * `beforeRouting = true`.
 *
 * {{{
 *   class UnsupportedMethodExceptionMapper
 *     extends ExceptionMapper[UnsupportedMethodException] {
 *     def toResponse(request: Request, throwable: UnsupportedMethodException): Response  = ???
 *   }
 *
 *   ...
 *
 *   val beforeRoutingExceptionManager: ExceptionManager =
 *     new ExceptionManager(injector, injector.instance[StatsReceiver])
 *
 *   beforeRoutingExceptionManager.add[UnsupportedMethodException]
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
 *      .exceptionMapper[UnsupportedMethodExceptionMapper]
 *    }
 * }}}
 *
 * @param message the detail message. The detail message is saved for later
 *                retrieval by the `#getMessage` method.
 */
class UnsupportedMethodException private[finatra] (message: String)
    extends Exception(message)
    with NoStackTrace
