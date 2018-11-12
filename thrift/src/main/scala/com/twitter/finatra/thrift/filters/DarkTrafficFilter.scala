package com.twitter.finatra.thrift.filters

import com.twitter.finagle.exp.AbstractDarkTrafficFilter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Singleton
import scala.reflect.ClassTag

/**
 * An implementation of [[com.twitter.finagle.exp.AbstractDarkTrafficFilter]] which extends
 * [[com.twitter.finatra.thrift.ThriftFilter]] and thus works in a Finatra ThriftRouter
 * filter chain. This differs from the [[com.twitter.finagle.exp.DarkTrafficFilter]] in that
 * this class is typed to work like other ThriftFilters as agnostic to types until apply() is
 * invoked.
 *
 * @note This Filter only works for Scala services. Java users should use the `JavaDarkTrafficFilter`.
 *
 * @param darkService ServiceIface to which to send requests.
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
  enableSampling: ThriftRequest[_] => Boolean,
  forwardAfterService: Boolean,
  override val statsReceiver: StatsReceiver
) extends ThriftFilter
    with AbstractDarkTrafficFilter
    with Logging {

  private val serviceIfaceClass = implicitly[ClassTag[ServiceIface]].runtimeClass

  def apply[T, Rep](
    request: ThriftRequest[T],
    service: Service[ThriftRequest[T], Rep]
  ): Future[Rep] = {
    if (forwardAfterService) {
      service(request).ensure {
        sendDarkRequest(request)(enableSampling, invokeMethod)
      }
    } else {
      serviceConcurrently(service, request)(enableSampling, invokeMethod)
    }
  }

  protected def handleFailedInvocation[Req](request: Req, t: Throwable): Unit = {
    debug(s"Request: $request to dark traffic service failed.")
    error(t.getMessage, t)
  }

  /* Private */

  /**
   * The [[com.twitter.finatra.thrift.ThriftFilter]] filter chain works on a Service[ThriftRequest[T] => Rep].
   * The ThriftRequest contains the method name to be invoked on the service thus we find via reflection the
   * method to invoke on the dark service.
   * @param request - [[com.twitter.finatra.thrift.ThriftRequest]] to send to dark service
   * @tparam T - the type param of the args in the [[com.twitter.finatra.thrift.ThriftRequest]]
   * @tparam Rep - the response type param of the service call.
   * @return a [[com.twitter.util.Future]] over the Rep type.
   */
  private def invokeMethod[T, Rep](request: ThriftRequest[T]): Future[Rep] = {
    val field = serviceIfaceClass.getDeclaredField(request.methodName)
    field.setAccessible(true)
    val service = field.get(this.darkServiceIface).asInstanceOf[Service[T, Rep]]
    service(request.args)
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
 * @param enableSampling if function returns true, the request will be forwarded.
 * @param forwardAfterService forward the request after the initial service has processed the request
 * @param statsReceiver keeps stats for requests forwarded, skipped and failed.
 *
 * @tparam T this Filter's request type, which is expected to be Array[Byte] at runtime.
 * @tparam U this Filter's and the dark service's response type, which is expected to both
 *           be Array[Byte] at runtime.
 *
 * @see [[com.twitter.finagle.ThriftMux.newService]]
 * @see [[com.twitter.finagle.exp.AbstractDarkTrafficFilter]]
 */
@Singleton
class JavaDarkTrafficFilter(
  darkService: Service[ThriftClientRequest, Array[Byte]],
  enableSampling: Function[Array[Byte], Boolean],
  forwardAfterService: Boolean,
  override val statsReceiver: StatsReceiver)
  extends Filter.TypeAgnostic
    with AbstractDarkTrafficFilter
    with Logging {

  def this(
    darkService: Service[ThriftClientRequest, Array[Byte]],
    enableSampling: com.twitter.util.Function[Array[Byte], Boolean],
    statsReceiver: StatsReceiver
  ) {
    this(
      darkService,
      enableSampling,
      forwardAfterService = true,
      statsReceiver
    )
  }

  override def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] =
    new Filter[Req, Rep, Req, Rep] {
      def apply(
        request: Req,
        service: Service[Req, Rep]
      ): Future[Rep] = {
        if (forwardAfterService) {
          service(request).ensure {
            sendDarkRequest(request)(wrappedEnableSampling, invokeMethod)
          }
        } else {
          serviceConcurrently(service, request)(wrappedEnableSampling, invokeMethod)
        }
      }
    }

  protected def handleFailedInvocation[Req](request: Req, t: Throwable): Unit = {
    debug(s"Request: $request to dark traffic service failed.")
    error(t.getMessage, t)
  }

  /* Private */

  private def wrappedEnableSampling[Req](request: Req): Boolean = {
    enableSampling(request.asInstanceOf[Array[Byte]])
  }

  private def invokeMethod[Req, Rep](bytes: Req): Future[Rep] = {
    val request = new ThriftClientRequest(bytes.asInstanceOf[Array[Byte]], false)
    this.darkService(request).map(_.asInstanceOf[Rep])
  }
}
