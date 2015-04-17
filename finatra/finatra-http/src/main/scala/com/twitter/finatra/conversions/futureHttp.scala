package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.future.RichFutureOption
import com.twitter.finatra.exceptions.{HttpException, NotFoundException}
import com.twitter.inject.Logging
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpResponseStatus

object futureHttp {

  /* -------------------------------------------------------- */
  implicit class RichHttpFutureOption[A](futureOption: Future[Option[A]]) {

    def valueOrNotFound(msg: String = ""): Future[A] = {
      RichFutureOption.getInnerOrElseFail(futureOption, NotFoundException.plainText(msg))
    }
  }

  /* -------------------------------------------------------- */
  implicit class RichHttpFuture[A](future: Future[A]) extends Logging {

    def httpRescue[ExceptionToRescue: Manifest](status: HttpResponseStatus, errors: Seq[String] = Seq(), log: String = ""): Future[A] = {
      future.rescue {
        case t if manifest[ExceptionToRescue].runtimeClass.isAssignableFrom(t.getClass) =>
          if (log.nonEmpty) {
            warn(log)
          }

          Future.exception(
            HttpException(status, errors: _*))
      }
    }
  }

}
