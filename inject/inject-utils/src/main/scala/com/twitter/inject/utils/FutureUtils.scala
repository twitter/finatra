package com.twitter.inject.utils

import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Duration, Future, Promise, Time}

object FutureUtils {

  /**
   * Note: Ordering of results is preserved
   */
  def sequentialMap[A, B](seq: Seq[A])(func: A => Future[B]): Future[Seq[B]] = {
    seq.foldLeft(Future.value(Vector.empty[B])) {
      case (futureResults, element) =>
        for {
          results <- futureResults
          result <- func(element)
        } yield results :+ result
    }
  }

  /**
   * Note: Ordering of results is NOT preserved
   */
  def collectMap[A, B](seq: Seq[A])(func: A => Future[B]): Future[Seq[B]] = {
    Future.collect {
      seq map func
    }
  }

  private val timer = DefaultTimer.twitter

  def scheduleFuture[T](offset: Duration)(func: => Future[T]): Future[T] = {
    val promise = new Promise[T]
    timer.schedule(Time.now + offset) {
      promise.become(func)
    }
    promise
  }

  def exceptionsToFailedFuture[T](func: => Future[T]): Future[T] = {
    Future(func).flatten
  }
}
