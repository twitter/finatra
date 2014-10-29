package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.future._
import com.twitter.finatra.test.Test
import com.twitter.util.Future

class FutureConversionsTest extends Test {

  def intToString(num: Int): String = num.toString

  def intToFutureString(num: Int): Future[String] = Future(num.toString)

  def intToOptionString(num: Int): Option[String] = Some(num.toString)

  def intToFutureOptionString(num: Int): Future[Option[String]] = Future(Some(num.toString))

  def unitToFutureOptionAbc(): Future[Option[String]] = Future(Some("abc"))

  "Future[Option[T]]" should {
    "#getInnerOrFail" in {
      assertFuture(
        Future(Some(1)) getInnerOrElseFail TestException,
        Future(1))
    }
    "#getInnerOrFail when empty" in {
      assertFailedFuture[TestException](
        Future(None) getInnerOrElseFail TestException)
    }
    "#getInnerOrElseFail when already failed" in {
      assertFailedFuture[TestException](
        Future.exception(ExistingException).getInnerOrElseFail(TestException))
    }
    "#mapInner" in {
      assertFuture(
        Future(Some(1)) mapInner intToString,
        Future(Some("1")))
    }
    "#mapInnerOpt" in {
      assertFuture[Option[String]](
        Future(Some(1)) mapInnerOpt intToOptionString,
        Future(Some("1")))
    }
    "#flatMapInner" in {
      assertFuture(
        Future(Some(1)) flatMapInner intToFutureString,
        Future(Some("1")))
    }
    "#flatMapInnerOpt" in {
      assertFuture(
        Future(Some(1)) flatMapInnerOpt intToFutureOptionString,
        Future(Some("1")))
    }
    "#flatMapIfUndefined when None" in {
      assertFuture(
        Future[Option[String]](None) flatMapIfUndefined { _ =>
          Future(Some("abc"))
        },
        Future(Some("abc")))
    }
    "#flatMapInnerOpt when Some" in {
      assertFuture(
        Future(Some("xyz")) flatMapIfUndefined { _ =>
          Future(Some("abc"))
        },
        Future(Some("xyz")))
    }
  }

  "Future[Seq[T]]" should {
    "#mapInner" in {
      assertFuture(
        Future(Seq(1)) mapInner intToString,
        Future(Seq("1")))
    }
    "#mapInnerOpt" in {
      assertFuture(
        Future(Seq(1)) mapInnerOpt intToOptionString,
        Future(Seq("1")))
    }
    "#flatMapInner" in {
      assertFuture(
        Future(Seq(1)) flatMapInner intToFutureString,
        Future(Seq("1")))
    }
    "#flatMapInnerOpt" in {
      assertFuture(
        Future(Seq(1)) flatMapInnerOpt intToFutureOptionString,
        Future(Seq("1")))
    }
    "#flatMapInnerOpt when empty" in {
      assertFuture(
        Future(Seq[Int]()) flatMapInnerOpt intToFutureOptionString,
        Future(Seq[Int]()))
    }
  }

  "Future[Option[Seq[T]]]" should {
    "#flattenInner with Some list" in {
      assertFuture(
        Future(Some(Seq(1))).flattenInner,
        Future(Seq(1)))
    }
    "#flattenInner with None" in {
      assertFuture(
        Future(None).flattenInner,
        Future(Seq()))
    }
  }

  "Future[Seq[Future[T]]]" should {
    "#flattenInner with Some list" in {
      assertFuture(
        Future(Seq(Future(1))).flattenInner,
        Future(Seq(1)))
    }
    "#flattenInner with None" in {
      assertFuture(
        Future(Seq()).flattenInner,
        Future(Seq()))
    }
  }

  "Future[T]" should {
    "#toBoolean returns true when Return" in {
      assertFuture(
        Future("asdf").toBoolean,
        Future(true))
    }
    "#toBoolean returns false when Throw" in {
      assertFuture(
        Future.exception(ExistingException).toBoolean,
        Future(false))
    }
    "#toOption returns Some when Return" in {
      assertFuture(
        Future("asdf").toOption,
        Future(Some("asdf")))
    }
    "#toBoolean returns None when Throw" in {
      assertFuture(
        Future.exception(ExistingException).toOption,
        Future(None))
    }
  }

  "Future[Boolean]" should {
    var ran = false

    def sideeffect = {
      ran = true
      Future.Unit
    }

    "#flatMapIfTrue when Return(true)" in {
      ran = false
      assertFuture(
        Future(true).flatMapIfTrue(sideeffect),
        Future.Unit)
      ran should equal(true)
    }

    "#flatMapIfTrue when Return(false)" in {
      ran = false
      assertFuture(
        Future(false).flatMapIfTrue(sideeffect),
        Future.Unit)
      ran should equal(false)
    }

    "#flatMapIfTrue when Throw" in {
      ran = false
      assertFailedFuture[ExistingException](
        Future.exception(ExistingException).flatMapIfTrue(sideeffect))
      ran should equal(false)
    }
  }

  object ExistingException extends ExistingException

  class ExistingException extends Exception

}
