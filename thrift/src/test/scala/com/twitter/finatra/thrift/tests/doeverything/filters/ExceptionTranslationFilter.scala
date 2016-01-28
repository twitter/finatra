package com.twitter.finatra.thrift.tests.doeverything.filters

import com.twitter.finagle.{TimeoutException, Service}
import com.twitter.finatra.thrift.thriftscala.ServerErrorCause.InternalServerError
import com.twitter.finatra.thrift.thriftscala.{ServerError, NoClientIdError, UnknownClientIdError, ClientError}
import com.twitter.finatra.thrift.thriftscala.ClientErrorCause.RequestTimeout
import com.twitter.finatra.thrift.{ThriftRequest, ThriftFilter}
import com.twitter.inject.Logging
import com.twitter.util.{NonFatal, Future}

class ExceptionTranslationFilter
  extends ThriftFilter
  with Logging {

  override def apply[T, U](request: ThriftRequest[T], service: Service[ThriftRequest[T], U]): Future[U] = {
    service(request).rescue {
      case e: TimeoutException =>
        Future.exception(
          ClientError(RequestTimeout, e.getMessage))
      case e: ClientError =>
        Future.exception(e)
      case e: UnknownClientIdError =>
        Future.exception(e)
      case e: NoClientIdError =>
        Future.exception(e)
      case NonFatal(e) =>
        error("Unhandled exception", e)
        Future.exception(
          ServerError(InternalServerError, e.getMessage))
    }
  }
}
