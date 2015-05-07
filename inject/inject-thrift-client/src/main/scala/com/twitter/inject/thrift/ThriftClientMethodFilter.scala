package com.twitter.inject.thrift

import com.twitter.finagle.SimpleFilter
import com.twitter.inject.thrift.internal.FinatraThriftClientRequest
import com.twitter.util._

abstract class ThriftClientMethodFilter[T] extends SimpleFilter[FinatraThriftClientRequest, T] {

  implicit class RichFuture[A](future: Future[A]) {
    def partialTransform(pf: PartialFunction[Try[A], Future[A]]): Future[A] = {
      future.transform {
        case ret@Return(r) if pf.isDefinedAt(ret) =>
          pf(ret)
        case thr@Throw(t) if pf.isDefinedAt(thr) =>
          pf(thr)
        case _ =>
          future
      }
    }
  }
}
