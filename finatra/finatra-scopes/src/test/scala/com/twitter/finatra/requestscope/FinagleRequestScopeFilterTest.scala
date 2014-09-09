package com.twitter.finatra.requestscope

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Await, Future}
import org.scalatest.FunSuite
import org.scalatest.matchers.MustMatchers
import org.specs2.mock.Mockito

class FinagleRequestScopeFilterTest
  extends FunSuite
  with MustMatchers
  with Mockito {

  val service = mock[Service[Request, Response]]
  val response = Response()
  service(any[Request]) returns Future.value(response)

  val finagleScope = new FinagleRequestScope()
  val filter = new FinagleRequestScopeFilter(finagleScope)

  test("TracingFilter: should trace Finagle version") {
    val composed = filter andThen service
    assert(
      Await.result(
        composed(Request())) === response)

    assert(
      Await.result(
        composed(Request())) === response)
  }
}
