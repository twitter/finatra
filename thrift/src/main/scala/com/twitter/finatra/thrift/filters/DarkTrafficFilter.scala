package com.twitter.finatra.thrift.filters

import com.twitter.finagle.Service
import com.twitter.finagle.exp.AbstractDarkTrafficFilter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.annotations.Experimental
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.inject.Logging
import com.twitter.util.Future
import scala.reflect.ClassTag

/**
 * An implementation of [[com.twitter.finagle.exp.AbstractDarkTrafficFilter]] which extends
 * [[com.twitter.finatra.thrift.ThriftFilter]] and thus works in a Finatra ThriftRouter
 * filter chain. This differs from the [[com.twitter.finagle.exp.DarkTrafficFilter]] in that
 * this class is typed to work like other ThriftFilters as agnostic to types until apply() is
 * invoked.
 *
 * @param darkServiceIface - Service to take dark traffic
 * @param enableSampling - if function returns true, the request will forward
 * @param forwardAfterService - forward the dark request after the service has processed the request
 *        instead of concurrently.
 * @param statsReceiver - keeps stats for requests forwarded, skipped and failed.
 * @tparam ServiceIface - the type of the Service to take dark traffic
 * @see [[com.twitter.finagle.exp.DarkTrafficFilter]]
 */
@Experimental
class DarkTrafficFilter[ServiceIface: ClassTag](
  darkServiceIface: ServiceIface,
  enableSampling: ThriftRequest[_] => Boolean,
  forwardAfterService: Boolean,
  override val statsReceiver: StatsReceiver
) extends ThriftFilter
    with AbstractDarkTrafficFilter
    with Logging {

  private val serviceIfaceClass = implicitly[ClassTag[ServiceIface]].runtimeClass

  override def apply[T, Rep](
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

  override protected def handleFailedInvocation(t: Throwable): Unit = {
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
    val service = field.get(darkServiceIface).asInstanceOf[Service[T, Rep]]
    service(request.args)
  }
}
