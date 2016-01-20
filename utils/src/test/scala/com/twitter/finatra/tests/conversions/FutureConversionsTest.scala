package com.twitter.finatra.tests.conversions

import com.twitter.finatra.conversions.future._
import com.twitter.inject.Test
import com.twitter.util.{Throw, Future, Return}

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
    "#filter" in {
      assertFuture(
        Future(Seq(0, 1, 2, 3)) filterInner { _ > 1 },
        Future(Seq(2, 3)))
    }
    "#headOption when seq of size 1" in {
      assertFuture(
        Future(Seq(0)).headOption,
        Future(Some(0)))
    }
    "#headOption when seq of size > 1" in {
      assertFuture(
        Future(Seq(0, 1, 2, 3)).headOption,
        Future(Some(0)))
    }
    "#headOption when empty" in {
      assertFuture(
        Future(Seq[Int]()).headOption,
        Future(None))
    }
    "#collectInner" in {
      assertFuture(
        Future(Seq("a", 1)).collectInner { case num: Int => num + 1 },
        Future(Seq(2)))
    }
    "#groupBySingleValue" in {
      assertFuture(
        Future(Seq("a", "aa", "bb", "ccc")).groupBySingleValue { _.size },
        Future(Map(
          1 -> "a",
          2 -> "bb",
          3 -> "ccc")))
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
    "#chainedOnFailure succeeds when Return" in {
      assertFuture(
        Future("asdf").chainedOnFailure { throwable =>
          fail("should not run when Return")
        },
        Future("asdf"))
    }
    "#chainedOnFailure succeeds when Throw" in {
      var chainedRan = false

      assertFailedFuture[ExistingException](
        Future.exception(ExistingException).chainedOnFailure { throwable =>
          assert(throwable == ExistingException)
          chainedRan = true
          Future.Unit
        })

      assert(chainedRan)
    }
    "#chainedOnFailure returns failed future when Throw" in {
      var chainedRan = false

      assertFailedFuture[ExistingException](
        Future.exception(ExistingException).chainedOnFailure { throwable =>
          assert(throwable == ExistingException)
          chainedRan = true
          Future.exception(new Exception("another exception"))
        })

      assert(chainedRan)
    }

    "#chainedOnFailure throws Exception when Throw" in {
      var chainedRan = false

      assertFailedFuture[ExistingException](
        Future.exception(ExistingException).chainedOnFailure { throwable =>
          assert(throwable == ExistingException)
          chainedRan = true
          throw new Exception("another exception")
        })

      assert(chainedRan)
    }

    "#transformException transforms Exception" in {
      assertFailedFuture[TestException](
        Future.exception(ExistingException).transformException { throwable =>
          new TestException
        }
      )
    }

    "#transformException returns successful future" in {
      assertFuture(
        Future(true).transformException { throwable =>
          new TestException
        },
        Future(true))
    }
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

  "Future[Seq[Seq[A]]]" should {
    "flattenInnerSeq" in {
      assertFuture(
        Future(Seq(Seq(), Seq("a", "b"), Seq("c"))).flattenInnerSeq,
        Future(Seq("a", "b", "c")))
    }
  }

  "Future[T]" should {
    "chainedOnFailure when success" in {
      assertFuture(
        Future(1).chainedOnFailure { e => Future(2).unit },
        Future(1))
    }

    "chainedOnFailure when failure" in {
      assertFuture(
        Future(1).chainedOnFailure { e => Future.exception(new RuntimeException("failure in chained")) },
        Future(1))
    }

    "partialTransform when success matches" in {
      assertFailedFuture[RuntimeException](
        Future(1).partialTransform {
          case Return(r) if r == 1 =>
            Future.exception(new RuntimeException("bad"))
        })
    }

    "partialTransform when success doesn't match" in {
      assertFuture(
        Future(2).partialTransform {
          case Return(r) if r == 1 =>
            Future.exception(new RuntimeException("bad"))
        },
        Future(2))
    }

    "partialTransform when failed" in {
      assertFailedFuture[TestException](
        Future.exception[String](ExistingException).partialTransform {
          case Throw(e: ExistingException) =>
            Future.exception(TestException)
        })
    }
  }

  object TestException extends TestException

  class TestException extends Exception

  object ExistingException extends ExistingException

  class ExistingException extends Exception

}
