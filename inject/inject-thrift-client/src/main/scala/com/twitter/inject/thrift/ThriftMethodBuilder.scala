package com.twitter.inject.thrift

import com.twitter.finagle.service.ResponseClassifier
import com.twitter.finagle.thrift.service.{Filterable, ReqRepServicePerEndpointBuilder, ServicePerEndpointBuilder}
import com.twitter.finagle.thriftmux.MethodBuilder
import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.conversions.string._
import com.twitter.inject.Injector
import com.twitter.inject.thrift.internal.filters.ThriftClientExceptionFilter
import com.twitter.scrooge.ThriftMethod
import com.twitter.util.Duration
import com.twitter.util.tunable.Tunable

/**
 * A builder for configuring a Finagle [[Service]] representation of a ThriftMethod. See:
 * [[https://twitter.github.io/scrooge/Finagle.html#id2 ServicePerEndpoint Client]] for more information.
 *
 * Filter ordering:
 * {{{
 *            Request              Response
 *              |                    A
 *              |                    |
 *              V                    |
 * +------------------------------------------------+
 * |           ThriftMethodBuilder filters          |
 * +------------------------------------------------+
 * |              MethodBuilder filters             |
 * +------------------------------------------------+
 * |                  Method Service                |
 * +------------------------------------------------+
 * }}}
 *
 */
final class ThriftMethodBuilderFactory[ServicePerEndpoint <: Filterable[ServicePerEndpoint]](
  injector: Injector,
  methodBuilder: MethodBuilder
)(
  implicit builder: ServicePerEndpointBuilder[ServicePerEndpoint]
) {

  def method[Req, Rep](method: ThriftMethod): ThriftMethodBuilder[ServicePerEndpoint, Req, Rep] =
    new ThriftMethodBuilder[ServicePerEndpoint, Req, Rep](
      injector,
      methodBuilder,
      method
    )
}

/**
 * Provides `ThriftMethod`-specific [[MethodBuilder]] functionality.
 * @param mb the underlying [[MethodBuilder]]
 * @param method the [[ThriftMethod]] over which configuration is applied.
 *
 * {{{
 * +-------------------------------------------------+
 * | ThriftMethodBuilder Exception filter            |
 * +-------------------------------------------------+
 * | ThriftMethodBuilder filters (in order added)    |
 * +-------------------------------------------------+
 * | MethodBuilder filters                           |
 * +-------------------------------------------------+
 * }}}
 */
final class ThriftMethodBuilder[ServicePerEndpoint <: Filterable[ServicePerEndpoint], Req, Rep] private[thrift] (
  injector: Injector,
  mb: MethodBuilder,
  method: ThriftMethod
)(
  implicit builder: ServicePerEndpointBuilder[ServicePerEndpoint]
) {

  // Mutable
  private[this] var methodBuilder = mb
  private[this] var filterChain: Filter[Req, Rep, Req, Rep] = Filter.identity
  private[this] var exceptionFilterImpl: Filter[Req, Rep, Req, Rep] =
    new ThriftClientExceptionFilter[Req, Rep](mb.label, method)

  /**
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.withTimeoutTotal(howLong: Duration)]]
   */
  def withTimeoutTotal(howLong: Duration): this.type = {
    methodBuilder = methodBuilder.withTimeoutTotal(howLong)
    this
  }

  /**
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.withTimeoutTotal(howLong: Tunable[Duration])]]
   */
  def withTimeoutTotal(howLong: Tunable[Duration]): this.type = {
    methodBuilder = methodBuilder.withTimeoutTotal(howLong)
    this
  }

  /**
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.withTimeoutPerRequest(howLong: Duration)]]
   */
  def withTimeoutPerRequest(howLong: Duration): this.type = {
    methodBuilder = methodBuilder.withTimeoutPerRequest(howLong)
    this
  }

  /**
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.withTimeoutPerRequest(howLong: Tunable[Duration])]]
   */
  def withTimeoutPerRequest(howLong: Tunable[Duration]): this.type = {
    methodBuilder = methodBuilder.withTimeoutPerRequest(howLong)
    this
  }

  /**
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.withRetryForClassifier(classifier: ResponseClassifier)]]
   */
  def withRetryForClassifier(classifier: ResponseClassifier): this.type = {
    methodBuilder = methodBuilder.withRetryForClassifier(classifier)
    this
  }

  /**
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.withRetryDisabled]]
   */
  def withRetryDisabled: this.type = {
    methodBuilder = methodBuilder.withRetryDisabled
    this
  }

  /**
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.idempotent(maxExtraLoad: Double)]]
   */
  def idempotent(maxExtraLoad: Double): this.type = {
    methodBuilder = methodBuilder.idempotent(maxExtraLoad)
    this
  }

  /**
   * @see [[com.twitter.finagle.thriftmux.MethodBuilder.idempotent(maxExtraLoad: Tunable[Double])]]
   */
  def idempotent(maxExtraLoad: Tunable[Double]): this.type = {
    methodBuilder = methodBuilder.idempotent(maxExtraLoad)
    this
  }

 /**
  * @see [[com.twitter.finagle.thriftmux.MethodBuilder.nonIdempotent]]
  */
  def nonIdempotent: this.type = {
    methodBuilder = methodBuilder.nonIdempotent
    this
  }

  /**
   * Install a [[com.twitter.finagle.Filter]]. This filter will be added to the end of the filter chain. That is, this
   * filter will be invoked AFTER any other installed filter on a request [[Req]] and thus BEFORE any other installed
   * filter on a response [[Rep]].
   *
   * @param filter the [[com.twitter.finagle.Filter]] to install.
   * @return [[ThriftMethodBuilder]]
   */
  def filtered(filter: Filter[Req, Rep, Req, Rep]): this.type = {
    filterChain = filterChain.andThen(filter)
    this
  }

  /**
   * Install a [[com.twitter.finagle.Filter]]. This filter will be added to the end of the filter chain. That is, this
   * filter will be invoked AFTER any other installed filter on a request [[Req]] and thus BEFORE any other installed
   * filter on a response [[Rep]].
   *
   * @tparam T [[Filter]] subtype of the filter to instantiate from the injector
   * @return [[ThriftMethodBuilder]]
   */
  def filtered[T <: Filter[Req, Rep, Req, Rep]: Manifest]: this.type =
    filtered(injector.instance[T])

  /**
   * Install a [[com.twitter.finagle.Filter]] that is agnostic to the [[ThriftMethod]] Req/Rep types. This allows for
   * use of more general filters that do not care about the [[ThriftMethod]] input and output types.
   *
   * @param filter the [[com.twitter.finagle.Filter.TypeAgnostic]] to install.
   * @return [[ThriftMethodBuilder]]
   */
  def withAgnosticFilter(filter: Filter.TypeAgnostic): this.type = {
    filterChain = filterChain.andThen(filter.toFilter[Req, Rep])
    this
  }

  /**
   * Install a [[com.twitter.finagle.Filter.TypeAgnostic]] that is agnostic to the [[ThriftMethod]] Req/Rep types. This allows for
   * use of more general filters that do not care about the [[ThriftMethod]] input and output types.
   *
   * @tparam T [[Filter.TypeAgnostic]] subtype of the filter to instantiate from the injector
   * @return [[ThriftMethodBuilder]]
   */
  def withAgnosticFilter[T <: Filter.TypeAgnostic: Manifest]: this.type = {
    withAgnosticFilter(injector.instance[T])
  }

  /**
   * Install a [[com.twitter.finagle.Filter]] specific to handling exceptions. This filter will be correctly positioned
   * in the filter chain near the top of the stack. This filter is generally used to mutate or alter the final response
   * [[Rep]] based on a returned exception. E.g., to translate a transport-level exception from Finagle to an
   * application-level exception.
   *
   * @param filter the [[com.twitter.finagle.Filter]] to install.
   * @return [[ThriftMethodBuilder]]
   */
  def withExceptionFilter(filter: Filter[Req, Rep, Req, Rep]): this.type = {
    exceptionFilterImpl = filter
    this
  }

  /**
   * Install a [[com.twitter.finagle.Filter]] specific to handling exceptions. This filter will be correctly positioned
   * in the filter chain near the top of the stack. This filter is generally used to mutate or alter the final response
   * [[Rep]] based on a returned exception. E.g., to translate a transport-level exception from Finagle to an
   * application-level exception.
   *
   * @tparam T the type of the filter to instantiate from the injector
   * @return [[ThriftMethodBuilder]]
   */
  def withExceptionFilter[T <: Filter[Req, Rep, Req, Rep]: Manifest]: this.type = {
    withExceptionFilter(injector.instance[T])
  }

  /**
   * Method-specific Filters are 'outside' of TypeAgnostic *and* the MethodBuilder filters
   *
   * The layer of indirection via the `AndThenService` is to allow for implementations that may wish
   * to intercept the invocation of the service.
   *
   * @return a [[com.twitter.finagle.Service]] configured from the applied MethodBuilder configuration
   *         and filters.
   */
  def service: Service[Req, Rep] = {
    val methodServiceImpl: Service[Req, Rep] = findMethodService(
      methodBuilder
        .servicePerEndpoint[ServicePerEndpoint](method.name)
    )

    val service = builder match {
      case _: ReqRepServicePerEndpointBuilder[ServicePerEndpoint] =>
        method
          .toReqRepServicePerEndpointService(
            methodServiceImpl.asInstanceOf[method.ReqRepFunctionType]
          )
          .asInstanceOf[Service[Req, Rep]]
      case _ =>
        method
          .toServicePerEndpointService(methodServiceImpl.asInstanceOf[method.FunctionType])
          .asInstanceOf[Service[Req, Rep]]
    }
    toFilter.andThen(service)
  }

  /* Private */

  private[thrift] def toFilter: Filter[Req, Rep, Req, Rep] = {
    exceptionFilterImpl
      .andThen(filterChain)
  }

  /* Find the given Service[T, U] method defined in the given implementation */
  private[this] def findMethodService[I <: Filterable[I]](
    implementation: I
  ): Service[Req, Rep] = {
    val methodOpt =
      implementation
        .getClass
        .getDeclaredMethods
        .find(_.getName == method.name.camelify) // converts a snake_case ThriftMethod.name to camelCase for reflection method lookup
    methodOpt match {
      case Some(serviceMethod) =>
        serviceMethod
          .invoke(implementation)
          .asInstanceOf[Service[Req, Rep]]
      case _ =>
        throw new IllegalArgumentException(
          s"ThriftMethod: ${method.name} not found in implementation: ${implementation.getClass.getName}"
        )
    }
  }
}
