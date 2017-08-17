package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.future._
import com.twitter.util.{Future, Return, Throw}

class FutureConversionsTest extends Test {

  def intToString(num: Int): String = num.toString

  def intToFutureString(num: Int): Future[String] = Future(num.toString)

  def intToOptionString(num: Int): Option[String] = Some(num.toString)

  def intToFutureOptionString(num: Int): Future[Option[String]] = Future(Some(num.toString))

  def unitToFutureOptionAbc(): Future[Option[String]] = Future(Some("abc"))

  test("Future[Option[T]]#getInnerOrFail") {
    assertFuture(Future(Some(1)) getInnerOrElseFail TestException, Future(1))
  }
  test("Future[Option[T]]#getInnerOrFail when empty") {
    assertFailedFuture[TestException](Future(None) getInnerOrElseFail TestException)
  }
  test("Future[Option[T]]#getInnerOrElseFail when already failed") {
    assertFailedFuture[TestException](
      Future.exception(ExistingException).getInnerOrElseFail(TestException)
    )
  }
  test("Future[Option[T]]#mapInner") {
    assertFuture(Future(Some(1)) mapInner intToString, Future(Some("1")))
  }
  test("Future[Option[T]]#mapInnerOpt") {
    assertFuture[Option[String]](Future(Some(1)) mapInnerOpt intToOptionString, Future(Some("1")))
  }
  test("Future[Option[T]]#flatMapInner") {
    assertFuture(Future(Some(1)) flatMapInner intToFutureString, Future(Some("1")))
  }
  test("Future[Option[T]]#flatMapInnerOpt") {
    assertFuture(Future(Some(1)) flatMapInnerOpt intToFutureOptionString, Future(Some("1")))
  }
  test("Future[Option[T]]#flatMapIfUndefined when None") {
    assertFuture(Future[Option[String]](None) flatMapIfUndefined { _ =>
      Future(Some("abc"))
    }, Future(Some("abc")))
  }
  test("Future[Option[T]]#flatMapInnerOpt when Some") {
    assertFuture(Future(Some("xyz")) flatMapIfUndefined { _ =>
      Future(Some("abc"))
    }, Future(Some("xyz")))
  }

  test("Future[Seq[T]]#mapInner") {
    assertFuture(Future(Seq(1)) mapInner intToString, Future(Seq("1")))
  }
  test("Future[Seq[T]]#mapInnerOpt") {
    assertFuture(Future(Seq(1)) mapInnerOpt intToOptionString, Future(Seq("1")))
  }
  test("Future[Seq[T]]#flatMapInner") {
    assertFuture(Future(Seq(1)) flatMapInner intToFutureString, Future(Seq("1")))
  }
  test("Future[Seq[T]]#flatMapInnerOpt") {
    assertFuture(Future(Seq(1)) flatMapInnerOpt intToFutureOptionString, Future(Seq("1")))
  }
  test("Future[Seq[T]]#flatMapInnerOpt when empty") {
    assertFuture(Future(Seq[Int]()) flatMapInnerOpt intToFutureOptionString, Future(Seq[Int]()))
  }
  test("Future[Seq[T]]#filter") {
    assertFuture(Future(Seq(0, 1, 2, 3)) filterInner { _ > 1 }, Future(Seq(2, 3)))
  }
  test("Future[Seq[T]]#headOption when seq of size 1") {
    assertFuture(Future(Seq(0)).headOption, Future(Some(0)))
  }
  test("Future[Seq[T]]#headOption when seq of size > 1") {
    assertFuture(Future(Seq(0, 1, 2, 3)).headOption, Future(Some(0)))
  }
  test("Future[Seq[T]]#headOption when empty") {
    assertFuture(Future(Seq[Int]()).headOption, Future(None))
  }
  test("Future[Seq[T]]#collectInner") {
    assertFuture(Future(Seq("a", 1)).collectInner { case num: Int => num + 1 }, Future(Seq(2)))
  }
  test("Future[Seq[T]]#groupBySingleValue") {
    assertFuture(
      Future(Seq("a", "aa", "bb", "ccc")).groupBySingleValue { _.length },
      Future(Map(1 -> "a", 2 -> "bb", 3 -> "ccc"))
    )
  }

  test("Future[Option[Seq[T]]]#flattenInner with Some list") {
    assertFuture(Future(Some(Seq(1))).flattenInner, Future(Seq(1)))
  }
  test("Future[Option[Seq[T]]]#flattenInner with None") {
    assertFuture(Future(None).flattenInner, Future(Seq()))
  }

  test("Future[Seq[Future[T]]]#flattenInner with Some list") {
    assertFuture(Future(Seq(Future(1))).flattenInner, Future(Seq(1)))
  }
  test("Future[Seq[Future[T]]]#flattenInner with None") {
    assertFuture(Future(Seq()).flattenInner, Future(Seq()))
  }

  test("Future[T]#toBoolean returns true when Return") {
    assertFuture(Future("asdf").toBoolean, Future(true))
  }

  test("Future[T]#toBoolean returns false when Throw") {
    assertFuture(Future.exception(ExistingException).toBoolean, Future(false))
  }

  test("Future[T]#toOption returns Some when Return") {
    assertFuture(Future("asdf").toOption, Future(Some("asdf")))
  }

  test("Future[T]#toBoolean returns None when Throw") {
    assertFuture(Future.exception(ExistingException).toOption, Future(None))
  }

  test("Future[T]#chainedOnFailure succeeds when Return") {
    assertFuture(Future("asdf").chainedOnFailure { throwable =>
      fail("should not run when Return")
    }, Future("asdf"))
  }

  test("Future[T]#chainedOnFailure succeeds when Throw") {
    var chainedRan = false

    assertFailedFuture[ExistingException](Future.exception(ExistingException).chainedOnFailure {
      throwable =>
        assert(throwable == ExistingException)
        chainedRan = true
        Future.Unit
    })

    assert(chainedRan)
  }

  test("Future[T]#chainedOnFailure returns failed future when Throw") {
    var chainedRan = false

    assertFailedFuture[ExistingException](Future.exception(ExistingException).chainedOnFailure {
      throwable =>
        assert(throwable == ExistingException)
        chainedRan = true
        Future.exception(new Exception("another exception"))
    })

    assert(chainedRan)
  }

  test("Future[T]#chainedOnFailure throws Exception when Throw") {
    var chainedRan = false

    assertFailedFuture[ExistingException](Future.exception(ExistingException).chainedOnFailure {
      throwable =>
        assert(throwable == ExistingException)
        chainedRan = true
        throw new Exception("another exception")
    })

    assert(chainedRan)
  }

  test("Future[T]#transformException transforms Exception") {
    assertFailedFuture[TestException](
      Future.exception(ExistingException).transformException { throwable =>
        new TestException
      }
    )
  }

  test("Future[T]#transformException returns successful future") {
    assertFuture(Future(true).transformException { throwable =>
      new TestException
    }, Future(true))
  }

  def chainedSuccessFunc(throwable: Throwable): Future[Unit] = {
    Future.Unit
  }

  def chainedFailedFunc(throwable: Throwable): Future[Unit] = {
    Future.exception(new Exception("oops"))
  }

  def chainedFailedThrowsFunc(throwable: Throwable): Future[Unit] = {
    throw new Exception("oops")
  }

  var ran = false

  def sideeffect = {
    ran = true
    Future.Unit
  }

  test("Future[Boolean]#flatMapIfTrue when Return(true)") {
    ran = false
    assertFuture(Future(true).flatMapIfTrue(sideeffect), Future.Unit)
    ran should equal(true)
  }

  test("Future[Boolean]#flatMapIfTrue when Return(false)") {
    ran = false
    assertFuture(Future(false).flatMapIfTrue(sideeffect), Future.Unit)
    ran should equal(false)
  }

  test("Future[Boolean]#flatMapIfTrue when Throw") {
    ran = false
    assertFailedFuture[ExistingException](
      Future.exception(ExistingException).flatMapIfTrue(sideeffect)
    )
    ran should equal(false)
  }

  test("Future[Seq[Seq[A]]]#flattenInnerSeq") {
    assertFuture(
      Future(Seq(Seq(), Seq("a", "b"), Seq("c"))).flattenInnerSeq,
      Future(Seq("a", "b", "c"))
    )
  }

  test("Future[T]#chainedOnFailure when success") {
    assertFuture(Future(1).chainedOnFailure { e =>
      Future(2).unit
    }, Future(1))
  }

  test("Future[T]#chainedOnFailure when failure") {
    assertFuture(Future(1).chainedOnFailure { e =>
      Future.exception(new RuntimeException("failure) chained"))
    }, Future(1))
  }

  test("Future[T]#partialTransform when success matches") {
    assertFailedFuture[RuntimeException](Future(1).partialTransform {
      case Return(r) if r == 1 =>
        Future.exception(new RuntimeException("bad"))
    })
  }

  test("Future[T]#partialTransform when success doesn't match") {
    assertFuture(Future(2).partialTransform {
      case Return(r) if r == 1 =>
        Future.exception(new RuntimeException("bad"))
    }, Future(2))
  }

  test("Future[T]#partialTransform when failed") {
    assertFailedFuture[TestException](Future.exception[String](ExistingException).partialTransform {
      case Throw(e: ExistingException) =>
        Future.exception(TestException)
    })
  }

  object TestException extends TestException

  class TestException extends Exception

  object ExistingException extends ExistingException

  class ExistingException extends Exception

}
