package com.twitter.finatra.conversions

import com.twitter.util.{Future, Return, Throw, Try}

object option {

  /* ---------------------------------- */
  object RichOption {

    //In companion so can be called from httpfuture.scala
    def toFutureOrFail[A](option: Option[A], throwable: Throwable) = {
      option match {
        case Some(returnVal) => Future.value(returnVal)
        case None => Future.exception(throwable)
      }
    }

    def toTryOrFail[A](option: Option[A], throwable: Throwable) = {
      option match {
        case Some(returnVal) => Return(returnVal)
        case None => Throw(throwable)
      }
    }
  }

  implicit class RichOption[A](val self: Option[A]) extends AnyVal {
    def toFutureOrFail(throwable: Throwable) = {
      RichOption.toFutureOrFail(self, throwable)
    }

    def toTryOrFail(throwable: Throwable): Try[A] = {
      RichOption.toTryOrFail(self, throwable)
    }

    def toFutureOrElse(orElse: A): Future[A] = self match {
      case Some(returnVal) => Future.value(returnVal)
      case None => Future.value(orElse)
    }
    
    def toFutureOrElse(orElse: Future[A]): Future[A] = self match {
      case Some(returnVal) => Future.value(returnVal)
      case None => orElse
    }

    /**
     * Creates a string using the passed in "format string" and the
     * defined Option as its argument (or empty string if None).
     * @param fmtStr the string to format
     * @return a formatted string using the Option value if defined,
     *         otherwise an empty-string.
     */
    def format(fmtStr: String) = self match {
      case Some(value) => fmtStr.format(value)
      case None => ""
    }
  }

  /* ---------------------------------- */
  implicit class RichOptionFuture[A](val self: Option[Future[A]]) extends AnyVal {
    def toFutureOption: Future[Option[A]] = {
      self match {
        case Some(future) => future map {Some(_)}
        case None => Future.None
      }
    }
  }

  /* ---------------------------------- */
  implicit class RichOptionMap[A, B](val self: Option[Map[A, B]]) extends AnyVal {
    def mapInnerValues[C](func: B => C): Option[Map[A, C]] = {
      for (map <- self) yield {
        map.mapValues(func)
      }
    }
  }
}