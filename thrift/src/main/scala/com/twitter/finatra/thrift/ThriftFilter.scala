package com.twitter.finatra.thrift

import com.twitter.finagle.thrift.{ClientId, MethodMetadata}
import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.util.Future

@deprecated("Use Filter.TypeAgnostic", "2018-11-20")
object ThriftFilter {
  val Identity = new ThriftFilter {
    override def toString: String = "ThriftFilter.Identity"
    override def apply[T, U](
      request: ThriftRequest[T],
      service: Service[ThriftRequest[T], U]
    ): Future[U] = service(request)
  }
}

/** AbstractThriftFilter for Java usage. */
@deprecated("Use Filter.TypeAgnostic", "2018-11-20")
abstract class AbstractThriftFilter extends ThriftFilter

@deprecated("Use Filter.TypeAgnostic", "2018-11-20")
trait ThriftFilter extends Filter.TypeAgnostic { legacy =>
  def apply[T, U](request: ThriftRequest[T], service: Service[ThriftRequest[T], U]): Future[U]
  override def toFilter[T, U]: Filter[T, U, T, U] = new SimpleFilter[T, U] {
    override def apply(request: T, service: Service[T, U]): Future[U] = {
      val methodName = MethodMetadata.current match {
        case Some(mm) => mm.methodName
        case None => null
      }
      legacy.apply(
        ThriftRequest(methodName, Trace.id, ClientId.current, request),
        Service.mk { request: ThriftRequest[T] => service(request.args) }
      )
    }
  }
}
