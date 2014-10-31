package com.twitter.finatra.requestscope

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Await, Future}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.specs2.mock.Mockito

// TODO (DATAAPI-888): Something is wrong with this test. I can muck
// around with the assertions and it will still pass! See inline
// comments.
@RunWith(classOf[JUnitRunner])
class FinagleRequestScopeFilterTest
  extends FunSuite
  with Mockito {

  val finagleRequestScope = mock[FinagleRequestScope]
  val response = mock[Response]
  val filter = new FinagleRequestScopeFilter(finagleRequestScope)

  test("verify enter and exit are called") {
    val service = Service.mk[Request, Response] { request: Request =>
      there was one(finagleRequestScope).enter()
      // this will pass: there was no(finagleRequestScope).enter() 
      there was no(finagleRequestScope).exit()
      // this will pass: there was one(finagleRequestScope).exit()
      Future(response)
    }

    val composed = filter andThen service

    assert(
      Await.result(
        composed(Request())) === response)

    there was one(finagleRequestScope).enter()
    there was one(finagleRequestScope).exit()
  }
}
