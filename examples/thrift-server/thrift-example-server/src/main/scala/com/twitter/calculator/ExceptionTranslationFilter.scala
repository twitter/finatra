package com.twitter.calculator

import com.twitter.finagle.{Service, SimpleFilter, TimeoutException}
import com.twitter.finatra.thrift.thriftscala.ClientErrorCause.RequestTimeout
import com.twitter.finatra.thrift.thriftscala.ServerErrorCause.InternalServerError
import com.twitter.finatra.thrift.thriftscala.{ClientError, NoClientIdError, ServerError, UnknownClientIdError}
import com.twitter.inject.Logging
import com.twitter.finatra.thrift.ThriftRequest
import com.twitter.util.{Future, NonFatal}
import javax.inject.Singleton

@Singleton
class ExceptionTranslationFilter
  extends SimpleFilter[ThriftRequest, Any]
  with Logging {

  override def apply(request: ThriftRequest, service: Service[ThriftRequest, Any]): Future[Any] = {
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
