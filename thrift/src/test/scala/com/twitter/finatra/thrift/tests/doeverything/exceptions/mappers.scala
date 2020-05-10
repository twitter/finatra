package com.twitter.finatra.thrift.tests.doeverything.exceptions

import com.twitter.doeverything.thriftscala.{Answer, DoEverythingException}
import com.twitter.finatra.thrift.exceptions.ExceptionMapper
import com.twitter.scrooge.Response
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class ReqRepBarExceptionMapper extends ExceptionMapper[BarException, Response[String]] {
  def handleException(throwable: BarException): Future[Response[String]] = {
    Future.value(Response("ReqRep BarException caught"))
  }
}

@Singleton
class ReqRepFooExceptionMapper extends ExceptionMapper[FooException, Response[String]] {
  def handleException(throwable: FooException): Future[Response[String]] = {
    Future.value(Response("ReqRep FooException caught"))
  }
}

@Singleton
class ReqRepDoEverythingExceptionMapper
    extends ExceptionMapper[DoEverythingException, Response[Answer]] {
  def handleException(throwable: DoEverythingException): Future[Response[Answer]] = {
    Future.value(Response(Answer("ReqRep DoEverythingException caught")))
  }
}

@Singleton
class DoEverythingExceptionMapper extends ExceptionMapper[DoEverythingException, Answer] {
  def handleException(throwable: DoEverythingException): Future[Answer] = {
    Future.value(Answer("DoEverythingException caught"))
  }
}

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
