package com.twitter.finatra.http.tests.conversions

import com.twitter.conversions.time._
import com.twitter.finagle._
import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.conversions.futureHttp._
import com.twitter.finatra.http.exceptions.{HttpException, NotFoundException}
import com.twitter.inject.Test
import com.twitter.util.{Await, Future}

class FutureHttpConversionsTest extends Test {

  "Future[Option[T]]" should {
    "#getInnerOrElseNotFound when Some" in {
      assertFuture(
        Future(Some(1)).valueOrNotFound("id 1 not found"),
        Future(1))
    }
    "#getInnerOrElseNotFound when None" in {
      assertFailedFuture[NotFoundException](
        Future(None).valueOrNotFound("not found"))
    }
    "#getInnerOrElseNotFound with msg when None" in {
      val thrown = assertFailedFuture[NotFoundException](
        Future(None).valueOrNotFound("msg"))
      thrown.errors should equal(Seq("msg"))
    }

    "httpRescue when success" in {
      val searchService = new SearchService(Future(123), Future("abc needle xyz"))
      Await.result(
        searchService.search("needle")) should equal("processed abc needle xyz")
    }

    "httpRescue when future 1 fails with timeoutexception" in {
      val searchService = new SearchService(
        future1Response = Future.exception(new IndividualRequestTimeoutException(5.seconds)),
        future2Response = Future("asdf"))

      assertSearchServiceFailure(
        searchService,
        HttpException(ServiceUnavailable, "AuthError #1"))
    }

    "httpRescue when future 2 fails with timeoutexception" in {
      val searchService = new SearchService(
        future1Response = Future(123),
        future2Response = Future.exception(new IndividualRequestTimeoutException(5.seconds)))

      assertSearchServiceFailure(
        searchService,
        HttpException(ServiceUnavailable, "SearchError #1"))
    }
  }

  def intToString(num: Int): String = num.toString

  def intToFutureString(num: Int): Future[String] = Future(num.toString)

  def intToOptionString(num: Int): Option[String] = Some(num.toString)

  def intToFutureOptionString(num: Int): Future[Option[String]] = Future(Some(num.toString))

  def assertSearchServiceFailure(searchService: SearchService, expectedException: Throwable) {
    val e = intercept[HttpException] {
      Await.result(
        searchService.search("processed needle"))
    }
    e should equal(expectedException)
  }

  /* Example class using httpRescue */

  class SearchService(future1Response: Future[Long], future2Response: Future[String]) {

    /* Public */

    def search(term: String): Future[String] = {
      for {
        token <- auth("bob", "secret")
          .httpRescue[TimeoutException](ServiceUnavailable, log = "auth service timeout", errors = Seq("AuthError #1"))
          .httpRescue[WriteException](ServiceUnavailable, log = "auth service write error", errors = Seq("AuthError #2"))
        searchResult <- searchByTerm(token, term)
          .httpRescue[TimeoutException](ServiceUnavailable, log = "search service timeout", errors = Seq("SearchError #1"))
      } yield processResult(searchResult)
    }

    /* Private */

    private def auth(user: String, pass: String): Future[Long] = {
      future1Response
    }

    private def searchByTerm(authToken: Long, term: String) = future2Response

    private def processResult(result: String): String = {
      "processed " + result
    }
  }

}

