package com.twitter.finatra.thrift.tests.doeverything.exceptions

import com.google.inject.Singleton
import com.twitter.finatra.thrift.exceptions.ExceptionMapper
import com.twitter.util.Future

@Singleton
class BarExceptionMapper extends ExceptionMapper[BarException, String] {
  def handleException(throwable: BarException): Future[String] = {
    Future.value("BarException caught")
  }
}

@Singleton
class FooExceptionMapper extends ExceptionMapper[FooException, String] {
  def handleException(throwable: FooException): Future[String] = {
    Future.value("FooException caught")
  }
}
