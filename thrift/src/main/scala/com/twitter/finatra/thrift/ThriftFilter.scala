package com.twitter.finatra.thrift

import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future

object ThriftFilter {
  val Identity = new ThriftFilter {
    def apply[T, Rep](request: ThriftRequest[T], svc: Service[ThriftRequest[T], Rep]) = svc(request)
  }
}

/**
 * A ThriftFilter is a SimpleFilter[ThriftRequest[T], Rep] which is polymorphic in T.  Such filters
 * can operate on any ThriftRequest.
 */
trait ThriftFilter { self =>

  def apply[T, Rep](request: ThriftRequest[T], svc: Service[ThriftRequest[T], Rep]): Future[Rep]

  final def toFilter[T, Rep]: Filter[ThriftRequest[T], Rep, ThriftRequest[T], Rep] =
    new Filter[ThriftRequest[T], Rep, ThriftRequest[T], Rep] {
      override def apply(
        request: ThriftRequest[T],
        svc: Service[ThriftRequest[T], Rep]
      ): Future[Rep] = self.apply(request, svc)
    }

  final def andThen(next: ThriftFilter) = new ThriftFilter {
    override def apply[T, Rep](
      request: ThriftRequest[T],
      svc: Service[ThriftRequest[T], Rep]
    ): Future[Rep] = self.apply(request, next.toFilter[T, Rep].andThen(svc))
  }
}
