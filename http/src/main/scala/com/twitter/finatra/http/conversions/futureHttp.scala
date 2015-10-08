package com.twitter.finatra.http.conversions

import com.twitter.finagle.http.Status
import com.twitter.finatra.conversions.future.RichFutureOption
import com.twitter.finatra.http.exceptions.{HttpException, NotFoundException}
import com.twitter.inject.Logging
import com.twitter.util.Future

object futureHttp {

  /* -------------------------------------------------------- */
  implicit class RichHttpFutureOption[A](futureOption: Future[Option[A]]) {

    def valueOrNotFound(msg: String = ""): Future[A] = {
      RichFutureOption.getInnerOrElseFail(futureOption, NotFoundException.plainText(msg))
    }
  }

  /* -------------------------------------------------------- */
  implicit class RichHttpFuture[A](future: Future[A]) extends Logging {

    def httpRescue[ExceptionToRescue: Manifest](status: Status, errors: Seq[String] = Seq(), log: String = ""): Future[A] = {
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
