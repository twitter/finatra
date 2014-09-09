package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.options.addToFutureOptionToOptionFuture
import com.twitter.finatra.conversions.seq._
import com.twitter.util.{Future, NonFatal, Return, Throw}
import grizzled.slf4j.Logger

object future {

  private val log = Logger(getClass)

  /* ---------------------------------------------------------- */
  /* Future[Option[A]] */
  object RichFutureOption {

    //static since reused in httpfuture.scala
    def getInnerOrElseFail[A](futureOption: Future[Option[A]], throwable: Throwable): Future[A] = {
      futureOption.transform {
        case Return(Some(value)) => Future.value(value)
        case Return(None) => Future.exception(throwable)
        case Throw(t) => Future.exception(t)
      }
    }
  }

  class RichFutureOption[A](futureOption: Future[Option[A]]) {

    def getInnerOrElseFail(throwable: Throwable): Future[A] = {
      RichFutureOption.getInnerOrElseFail(futureOption, throwable)
    }

    def mapInner[B](func: A => B): Future[Option[B]] = {
      for (option <- futureOption) yield {
        option map func
      }
    }

    def mapInnerOpt[B](func: A => Option[B]): Future[Option[B]] = {
      for (option <- futureOption) yield {
        option flatMap func
      }
    }

    def flatMapInner[B](func: A => Future[B]): Future[Option[B]] = {
      futureOption mapInner func flatMap {_.toFutureOption}
    }

    def flatMapInnerIf(cond: Boolean)(func: A => Future[A]): Future[Option[A]] = {
      if (!cond)
        futureOption
      else
        futureOption mapInner func flatMap {_.toFutureOption}
    }

    def flatMapInnerOpt[B](func: A => Future[Option[B]]): Future[Option[B]] = {
      futureOption flatMapInner func map {_.getOrElse(None)}
    }

    def flatMapIfUndefined(func: Unit => Future[Option[A]]): Future[Option[A]] = {
      futureOption flatMap {
        case Some(_) => futureOption
        case _ => func()
      }
    }
  }

  implicit def enrichFutureOption[A](futureOption: Future[Option[A]]): RichFutureOption[A] = new RichFutureOption[A](futureOption)

  /* ---------------------------------------------------------- */
  /* Future[Option[Seq[A]]] */

  class RichFutureOptionSeq[A](futureOptionSeq: Future[Option[Seq[A]]]) {
    def flattenInner: Future[Seq[A]] = {
      for (optionSeq <- futureOptionSeq) yield {
        optionSeq getOrElse Seq()
      }
    }
  }

  implicit def addFlattenToFutureOptionSeq[A](futureOptionSeq: Future[Option[Seq[A]]]): RichFutureOptionSeq[A] = new RichFutureOptionSeq[A](futureOptionSeq)

  /* ---------------------------------------------------------- */
  /* Future[Seq[Seq[A]]] */

  class RichFutureSeqSeq[A](futureSeqSeq: Future[Seq[Seq[A]]]) {
    def flattenInnerSeq: Future[Seq[A]] = {
      for (seqSeq <- futureSeqSeq) yield {
        seqSeq.flatten
      }
    }
  }

  implicit def addFlattenToFutureSeqSeq[A](futureSeqSeq: Future[Seq[Seq[A]]]): RichFutureSeqSeq[A] = new RichFutureSeqSeq[A](futureSeqSeq)

  /* ---------------------------------------------------------- */
  /* Future[Seq[A]] */

  class RichFutureSeq[A](futureSeq: Future[Seq[A]]) {

    def mapInner[B](func: A => B): Future[Seq[B]] = {
      for (seq <- futureSeq) yield {
        seq map func
      }
    }

    def mapInnerOpt[B](func: A => Option[B]): Future[Seq[B]] = {
      for (seq <- futureSeq) yield {
        seq map func flatMap {_.toIterable}
      }
    }

    def collectInner[B](pf: PartialFunction[A, B]): Future[Seq[B]] = {
      for (seq <- futureSeq) yield {
        seq collect pf
      }
    }

    def flatMapInner[B](func: A => Future[B]): Future[Seq[B]] = {
      mapInner(func) flatMap {Future.collect(_)}
    }

    def flatMapInnerOpt[B](func: A => Future[Option[B]]): Future[Seq[B]] = {
      mapInner(func) flatMap {Future.collect(_)} map {_.flatten}
    }

    def groupBySingleValue[B](func: A => B): Future[Map[B, A]] = {
      for (seq <- futureSeq) yield {
        seq groupBySingleValue func
      }
    }
  }

  implicit def enrichFutureSeq[A](futureSeq: Future[Seq[A]]): RichFutureSeq[A] = new RichFutureSeq[A](futureSeq)

  /* ---------------------------------------------------------- */
  /* Future[Seq[Future[A]] */

  class RichFutureSeqFutures[A](futureSeqFutures: Future[Seq[Future[A]]]) {

    def flattenInner: Future[Seq[A]] = {
      futureSeqFutures flatMap {Future.collect(_)}
    }
  }

  implicit def addFlattenToFutureSeqFutures[A](futureSeqFutures: Future[Seq[Future[A]]]): RichFutureSeqFutures[A] = new RichFutureSeqFutures[A](futureSeqFutures)

  /* ---------------------------------------------------------- */
  /* Future[Boolean] */

  class RichFutureBoolean(futureBoolean: Future[Boolean]) {

    def flatMapIfTrue(func: => Future[Unit]): Future[Unit] = {
      futureBoolean flatMap { boolean =>
        if (boolean)
          func
        else
          Future.Unit
      }
    }
  }

  implicit def futureBooleanToRichFutureBoolean[A](futureBoolean: Future[Boolean]): RichFutureBoolean = new RichFutureBoolean(futureBoolean)

  /* ---------------------------------------------------------- */
  /* Future[T] */

  class RichFuture[A](future: Future[A]) {

    def chainedOnFailure(failureFunc: Throwable => Future[Unit]): Future[A] = {
      future rescue {
        case NonFatal(primaryThrowable) =>
          failureFunc(primaryThrowable) transform {
            case Throw(secondaryThrowable) =>
              log.error("Additional failure in chainedOnFailure", secondaryThrowable)
              future
            case _ =>
              future
          }
      }
    }

    def toOption: Future[Option[A]] = {
      future map Some.apply handle { case e => None}
    }

    def toBoolean: Future[Boolean] = {
      future map { _ => true} handle { case e => false}
    }
  }

  implicit def futureToRichFuture[A](future: Future[A]): RichFuture[A] = new RichFuture[A](future)
}
