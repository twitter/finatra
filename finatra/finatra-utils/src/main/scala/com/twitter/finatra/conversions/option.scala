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

  implicit class RichOption[A](wrapped: Option[A]) {
    def toFutureOrFail(throwable: Throwable) = {
      RichOption.toFutureOrFail(wrapped, throwable)
    }

    def toTryOrFail(throwable: Throwable): Try[A] = {
      RichOption.toTryOrFail(wrapped, throwable)
    }

    def toFutureOrElse(orElse: A): Future[A] = wrapped match {
      case Some(returnVal) => Future.value(returnVal)
      case None => Future.value(orElse)
    }
    
    def toFutureOrElse(orElse: Future[A]): Future[A] = wrapped match {
      case Some(returnVal) => Future.value(returnVal)
      case None => orElse
    }

    /* Creates a string using the passed in "format string" and the defined Option as its argument (or empty string if None) */
    def format(fmtStr: String) = wrapped match {
      case Some(value) => fmtStr.format(value)
      case None => ""
    }
  }

  /* ---------------------------------- */
  implicit class RichOptionFuture[A](optionFuture: Option[Future[A]]) {
    def toFutureOption: Future[Option[A]] = {
      optionFuture match {
        case Some(future) => future map {Some(_)}
        case None => Future.None
      }
    }
  }

  /* ---------------------------------- */
  implicit class RichOptionMap[A, B](mapOpt: Option[Map[A, B]]) {
    def mapInnerValues[C](func: B => C): Option[Map[A, C]] = {
      for (map <- mapOpt) yield {
        map.mapValues(func)
      }
    }
  }
}