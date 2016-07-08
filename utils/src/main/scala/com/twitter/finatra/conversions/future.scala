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
    def getInnerOrElseFail[A](self: Future[Option[A]], throwable: => Throwable): Future[A] = {
      self.transform {
        case Return(Some(value)) => Future.value(value)
        case Return(None) => Future.exception(throwable)
        case Throw(origThrowable) =>
          log.warn("Failed future " + origThrowable + " converted into " + throwable)
          Future.exception(throwable)
      }
    }
  }

  implicit class RichFutureOption[A](val self: Future[Option[A]]) extends AnyVal {

    def getInnerOrElseFail(throwable: => Throwable): Future[A] = {
      RichFutureOption.getInnerOrElseFail(self, throwable)
    }

    def mapInner[B](func: A => B): Future[Option[B]] = {
      for (option <- self) yield {
        option map func
      }
    }

    def mapInnerOpt[B](func: A => Option[B]): Future[Option[B]] = {
      for (option <- self) yield {
        option flatMap func
      }
    }

    def flatMapInner[B](func: A => Future[B]): Future[Option[B]] = {
      self mapInner func flatMap {_.toFutureOption}
    }

    def flatMapInnerOpt[B](func: A => Future[Option[B]]): Future[Option[B]] = {
      self flatMapInner func map {_.getOrElse(None)}
    }

    def flatMapIfUndefined(func: Unit => Future[Option[A]]): Future[Option[A]] = {
      self flatMap {
        case Some(_) => self
        case _ => func(())
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Option[Seq[A]]] */

  implicit class RichFutureOptionSeq[A](val self: Future[Option[Seq[A]]]) extends AnyVal {
    def flattenInner: Future[Seq[A]] = {
      for (optionSeq <- self) yield {
        optionSeq getOrElse Seq()
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Seq[Seq[A]]] */

  implicit class RichFutureSeqSeq[A](val self: Future[Seq[Seq[A]]]) extends AnyVal {
    def flattenInnerSeq: Future[Seq[A]] = {
      for (seqSeq <- self) yield {
        seqSeq.flatten
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Seq[A]] */

  implicit class RichFutureSeq[A](val self: Future[Seq[A]]) extends AnyVal {

    def mapInner[B](func: A => B): Future[Seq[B]] = {
      for (seq <- self) yield {
        seq map func
      }
    }

    def mapInnerOpt[B](func: A => Option[B]): Future[Seq[B]] = {
      for (seq <- self) yield {
        seq map func flatMap {_.toIterable}
      }
    }

    def collectInner[B](pf: PartialFunction[A, B]): Future[Seq[B]] = {
      for (seq <- self) yield {
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
      for (seq <- self) yield {
        seq groupBySingleValue func
      }
    }

    def filterInner(func: A => Boolean): Future[Seq[A]] = {
      for (seq <- self) yield {
        seq filter func
      }
    }

    def headOption: Future[Option[A]] = {
      for (seq <- self) yield {
        seq.headOption
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Seq[Future[A]] */

  implicit class RichFutureSeqFutures[A](val self: Future[Seq[Future[A]]]) extends AnyVal {

    def flattenInner: Future[Seq[A]] = {
      self flatMap {Future.collect(_)}
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[Boolean] */

  implicit class RichFutureBoolean(val self: Future[Boolean]) extends AnyVal {

    def flatMapIfTrue(func: => Future[Unit]): Future[Unit] = {
      self flatMap { boolean =>
        if (boolean)
          func
        else
          Future.Unit
      }
    }
  }

  /* ---------------------------------------------------------- */
  /* Future[T] */

  implicit class RichFuture[A](val self: Future[A]) extends AnyVal {

    def chainedOnFailure(failureFunc: Throwable => Future[Unit]): Future[A] = {
      self rescue {
        case NonFatal(primaryThrowable) =>
          Future(failureFunc(primaryThrowable)).flatten transform {
            case Throw(secondaryThrowable) =>
              log.error("Additional failure in chainedOnFailure", secondaryThrowable)
              self
            case _ =>
              self
          }
      }
    }

    def transformException(f: Throwable => Throwable): Future[A] = {
      self transform {
        case Throw(t) =>
          Future.exception(f(t))
        case _ =>
          self
      }
    }

    def toOption: Future[Option[A]] = {
      self map Some.apply handle {
        case NonFatal(e) =>
          log.warn(e)
          None
      }
    }

    def toBoolean: Future[Boolean] = {
      self map { _ => true } handle {
        case NonFatal(e) =>
          log.warn(e)
          false
      }
    }

    def partialTransform(pf: PartialFunction[Try[A], Future[A]]): Future[A] = {
      self.transform {
        case ret@Return(r) if pf.isDefinedAt(ret) =>
          pf(ret)
        case thr@Throw(t) if pf.isDefinedAt(thr) =>
          pf(thr)
        case _ =>
          self
      }
    }
  }

}
