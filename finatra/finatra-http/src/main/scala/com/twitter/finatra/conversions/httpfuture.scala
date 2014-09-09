package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.future.RichFutureOption
import com.twitter.finatra.exceptions.{HttpException, NotFoundException}
import com.twitter.finatra.utils.Logging
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpResponseStatus

object httpfuture {

  /* -------------------------------------------------------- */
  class RichFutureOption[A](futureOption: Future[Option[A]]) {

    def valueOrNotFound(msg: String = ""): Future[A] = {
      RichFutureOption.getInnerOrElseFail(futureOption, NotFoundException.plainText(msg))
    }
  }

  implicit def richFutureOption[A](futureOption: Future[Option[A]]): RichFutureOption[A] = new RichFutureOption[A](futureOption)

  /* -------------------------------------------------------- */
  class RichFuture[A](future: Future[A]) extends Logging {

    def httpRescue[ExceptionToRescue: Manifest](status: HttpResponseStatus, errors: Seq[String] = Seq(), log: String = ""): Future[A] = {
      future.rescue {
        case t if manifest[ExceptionToRescue].erasure.isAssignableFrom(t.getClass) =>
          if (log.nonEmpty) {
            warn(log)
          }

          Future.exception(
            HttpException(status, errors: _*))
      }
    }
  }

  implicit def richFuture[A](future: Future[A]): RichFuture[A] = new RichFuture[A](future)
}
