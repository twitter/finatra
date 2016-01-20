package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.option._
import com.twitter.finatra.conversions.seq._
import com.twitter.util._
import grizzled.slf4j.Logger

object future {

  private val log = Logger(getClass)

  /* ---------------------------------------------------------- */
  /* Future[Option[A]] */
  object RichFutureOption {

    //static since reused in httpfuture.scala
    def getInnerOrElseFail[A](futureOption: Future[Option[A]], throwable: => Throwable): Future[A] = {
      futureOption.transform {
        case Return(Some(value)) => Future.value(value)
        case Return(None) => Future.exception(throwable)
        case Throw(origThrowable) =>
          log.warn("Failed future " + origThrowable + " converted into " + throwable)
          Future.exception(throwable)
      }
    }
  }

  implicit class RichFutureOption[A](futureOption: Future[Option[A]]) {

    def getInnerOrElseFail(throwable: => Throwable): Future[A] = {
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

    def flatMapInnerOpt[B](func: A => Future[Option[B]]): Future[Option[B]] = {
      futureOption flatMapInner func map {_.getOrElse(None)}
    }

    def flatMapIfUndefined(func: Unit => Future[Option[A]]): Future[Option[A]] = {
      futureOption flatMap {
        case Some(_) => futureOption
        case _ => func(())
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Option[Seq[A]]] */

  implicit class RichFutureOptionSeq[A](futureOptionSeq: Future[Option[Seq[A]]]) {
    def flattenInner: Future[Seq[A]] = {
      for (optionSeq <- futureOptionSeq) yield {
        optionSeq getOrElse Seq()
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Seq[Seq[A]]] */

  implicit class RichFutureSeqSeq[A](futureSeqSeq: Future[Seq[Seq[A]]]) {
    def flattenInnerSeq: Future[Seq[A]] = {
      for (seqSeq <- futureSeqSeq) yield {
        seqSeq.flatten
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Seq[A]] */

  implicit class RichFutureSeq[A](futureSeq: Future[Seq[A]]) {

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

    def filterInner(func: A => Boolean): Future[Seq[A]] = {
      for (seq <- futureSeq) yield {
        seq filter func
      }
    }

    def headOption: Future[Option[A]] = {
      for (seq <- futureSeq) yield {
        seq.headOption
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Seq[Future[A]] */

  implicit class RichFutureSeqFutures[A](futureSeqFutures: Future[Seq[Future[A]]]) {

    def flattenInner: Future[Seq[A]] = {
      futureSeqFutures flatMap {Future.collect(_)}
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Boolean] */

  implicit class RichFutureBoolean(futureBoolean: Future[Boolean]) {

    def flatMapIfTrue(func: => Future[Unit]): Future[Unit] = {
      futureBoolean flatMap { boolean =>
        if (boolean)
          func
        else
          Future.Unit
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[T] */

  implicit class RichFuture[A](future: Future[A]) {

    def chainedOnFailure(failureFunc: Throwable => Future[Unit]): Future[A] = {
      future rescue {
        case NonFatal(primaryThrowable) =>
          Future(failureFunc(primaryThrowable)).flatten transform {
            case Throw(secondaryThrowable) =>
              log.error("Additional failure in chainedOnFailure", secondaryThrowable)
              future
            case _ =>
              future
          }
      }
    }

    def transformException(f: Throwable => Throwable): Future[A] = {
      future transform {
        case Throw(t) =>
          Future.exception(f(t))
        case _ =>
          future
      }
    }

    def toOption: Future[Option[A]] = {
      future map Some.apply handle {
        case NonFatal(e) =>
          log.warn(e)
          None
      }
    }

    def toBoolean: Future[Boolean] = {
      future map { _ => true } handle {
        case NonFatal(e) =>
          log.warn(e)
          false
      }
    }

    def partialTransform(pf: PartialFunction[Try[A], Future[A]]): Future[A] = {
      future.transform {
        case ret@Return(r) if pf.isDefinedAt(ret) =>
          pf(ret)
        case thr@Throw(t) if pf.isDefinedAt(thr) =>
          pf(thr)
        case _ =>
          future
      }
    }
  }

}
