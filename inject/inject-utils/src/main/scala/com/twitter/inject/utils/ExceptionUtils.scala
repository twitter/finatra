package com.twitter.inject.utils

import com.twitter.finagle.{FailedFastException, SourcedException, TimeoutException}
import com.twitter.util.Throwables._
import com.twitter.util.{Throw, Try}
import org.apache.commons.lang.StringUtils._

object ExceptionUtils {

  def stripNewlines(e: Throwable): String = {
    stripNewlines(e.toString)
  }

  def stripNewlines(str: String): String = {
    replace(str, "\n\twith NoSources", "")
  }

  def toExceptionDetails(exception: Throwable): String = {
    mkString(exception).mkString("/")
  }

  def toExceptionMessage(tryThrowable: Try[_]): String = tryThrowable match {
    case Throw(e) => toExceptionMessage(e)
    case _ => ""
  }

  def toExceptionMessage(exception: Throwable): String = exception match {
    case e: TimeoutException =>
      e.exceptionMessage
    case e: FailedFastException =>
      e.getClass.getName
    case e: SourcedException =>
      stripNewlines(e)
    case e =>
      val msg = e.getMessage
      if (msg == null || msg.isEmpty)
        e.getClass.getName
      else
        e.getClass.getName + " " + msg
  }

  def toDetailedExceptionMessage(tryThrowable: Try[_]): String = tryThrowable match {
    case Throw(e) => toExceptionDetails(e) + " " + toExceptionMessage(e)
    case _ => ""
  }
}
