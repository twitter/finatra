package com.twitter.inject.thrift.filters

import com.twitter.conversions.StringOps._
import com.twitter.finagle.exp.AbstractDarkTrafficFilter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.{MethodMetadata, ThriftClientRequest}
import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Singleton
import scala.reflect.ClassTag

sealed abstract class BaseDarkTrafficFilter(
  forwardAfterService: Boolean,
  override val statsReceiver: StatsReceiver)
    extends Filter.TypeAgnostic
    with AbstractDarkTrafficFilter
    with Logging {

  protected def invokeDarkService[T, U](request: T): Future[U]

  protected def enableSampling: Any => Boolean

  protected def handleFailedInvocation[T](request: T, throwable: Throwable): Unit = {
    debug(s"Request: $request to dark traffic service failed.")
    error(throwable.getMessage, throwable)
  }

  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {
    def apply(request: T, service: Service[T, U]): Future[U] = {
      if (forwardAfterService) {
        service(request).ensure {
          sendDarkRequest(request)(enableSampling, invokeDarkService)
        }
      } else {
        serviceConcurrently(service, request)(enableSampling, invokeDarkService)
      }
    }
  }
}

/**
 * An implementation of [[com.twitter.finagle.exp.AbstractDarkTrafficFilter]] which extends
 * [[com.twitter.finagle.Filter.TypeAgnostic]] and thus works in a Finatra ThriftRouter
 * filter chain. This differs from the [[com.twitter.finagle.exp.DarkTrafficFilter]] in that
 * this class is typed to work like other ThriftFilters as agnostic to types until apply() is
 * invoked.
 *
 * @note This Filter only works for Scala services. Java users should use the `JavaDarkTrafficFilter`.
 *
 * @param darkServiceIface ServiceIface to which to send requests.
 * @param enableSampling if function returns true, the request will be forwarded.
 * @param forwardAfterService forward the request after the initial service has processed the request
 *        instead of concurrently.
 * @param statsReceiver keeps stats for requests forwarded, skipped and failed.
 *
 * @tparam ServiceIface - the type of the Service to take dark traffic.
 *
 * @see [[com.twitter.finagle.exp.AbstractDarkTrafficFilter]]
 */
@Singleton
class DarkTrafficFilter[ServiceIface: ClassTag](
  darkServiceIface: ServiceIface,
  override protected val enableSampling: Any => Boolean,
  forwardAfterService: Boolean,
  override val statsReceiver: StatsReceiver,
  lookupByMethod: Boolean = false)
    extends BaseDarkTrafficFilter(forwardAfterService, statsReceiver) {

  private val serviceIfaceClass = implicitly[ClassTag[ServiceIface]].runtimeClass

  private def getService(methodName: String): AnyRef = {
    if (lookupByMethod) {
      val field = serviceIfaceClass.getMethod(methodName)
      field.invoke(this.darkServiceIface)
    } else {
      val field = serviceIfaceClass.getDeclaredField(methodName)
      field.setAccessible(true)
      field.get(this.darkServiceIface)
    }
  }

  /**
   * The [[com.twitter.finagle.Filter.TypeAgnostic]] filter chain works on a Service[T, Rep].
   * The method name is extracted from the local context.
   * @param request - the request to send to dark service
   * @tparam T - the request type
   * @tparam Rep - the response type param of the service call.
   * @return a [[com.twitter.util.Future]] over the Rep type.
   */
  protected def invokeDarkService[T, Rep](request: T): Future[Rep] = {
    MethodMetadata.current match {
      case Some(mm) =>
        val service =
          getService(mm.methodName.toCamelCase)
            .asInstanceOf[Service[T, Rep]] // converts a snake_case MethodMetadata.methodName to camelCase for reflection method lookup
        service(request)
      case None =>
        val t = new IllegalStateException("DarkTrafficFilter invoked without method data")
        error(t)
        Future.exception(t)
    }
  }
}

/**
 * An implementation of [[com.twitter.finagle.exp.AbstractDarkTrafficFilter]] which extends
 * [[com.twitter.finagle.Filter.TypeAgnostic]] for use with generated Java code.
 *
 * @note This filter is expected to be applied on a `Service[Array[Byte], Array[Byte]]`
 *
 * @param darkService Service to which to send requests. Expected to be
 *                 `Service[ThriftClientRequest, Array[Byte]]` which is the return
 *                 from `ThriftMux.newService`.
 * @param enableSamplingFn if function returns true, the request will be forwarded.
 * @param forwardAfterService forward the request after the initial service has processed the request
 * @param statsReceiver keeps stats for requests forwarded, skipped and failed.
 *
 * @see [[com.twitter.finagle.ThriftMux.newService]]
 * @see [[com.twitter.finagle.exp.AbstractDarkTrafficFilter]]
 */
@Singleton
class JavaDarkTrafficFilter(
  darkService: Service[ThriftClientRequest, Array[Byte]],
  enableSamplingFn: Function[Array[Byte], Boolean],
  forwardAfterService: Boolean,
  override val statsReceiver: StatsReceiver)
    extends BaseDarkTrafficFilter(forwardAfterService, statsReceiver) {

  def this(
    darkService: Service[ThriftClientRequest, Array[Byte]],
    enableSamplingFn: com.twitter.util.Function[Array[Byte], Boolean],
    statsReceiver: StatsReceiver
  ) {
    this(darkService, enableSamplingFn, forwardAfterService = true, statsReceiver)
  }

  override protected val enableSampling: Any => Boolean = { request: Any =>
    enableSamplingFn(request.asInstanceOf[Array[Byte]])
  }

  override protected def invokeDarkService[Req, Rep](bytes: Req): Future[Rep] = {
    val request = new ThriftClientRequest(bytes.asInstanceOf[Array[Byte]], false)
    this.darkService(request).map(_.asInstanceOf[Rep])
  }
}
