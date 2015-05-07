package com.twitter.inject.thrift.internal

import com.twitter.finagle.{Failure, Service}
import com.twitter.inject.thrift.ThriftClientMethodFilter
import com.twitter.util.{Future, Return}

/**
 * Filter which converts successful futures matching the isFailure predicate, into failed Futures
 * using the com.twitter.finagle.Failure exception
 *
 * @param isFailure Predicate to determine if successful future results are actually failures
 * @param msg Message for use in com.twitter.finagle.Failure
 * @param flags Flags for use in com.twitter.finagle.Failure
 * @tparam T
 */
class MethodResultToFailureFilter[T](
  isFailure: T => Boolean,
  msg: => String,
  flags: => Long)
  extends ThriftClientMethodFilter[T] {

  override def apply(request: FinatraThriftClientRequest, service: Service[FinatraThriftClientRequest, T]): Future[T] = {
    service(request) transform {
      case Return(r) if isFailure(r) =>
        Future.exception(
          Failure(msg, flags))
      case tr =>
        Future.const(tr)
    }
  }
}
