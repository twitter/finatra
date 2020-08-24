package com.twitter.inject.server

import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.Logging
import com.twitter.util.{Await, Awaitable, Duration}
import java.util.concurrent.CountDownLatch

private[server] object Awaiter extends Logging {

  /**
   * Awaits for ''any'' [[Awaitable]] to be ready (exit) and will then exit. This is
   * different than [[Await.all]] which only exits once ''all'' Awaitables are ready.
   * @param awaitables the list of [[Awaitable]]s to await.
   * @param period the interval on which to check each [[Awaitable.isReady]]
   */
  def any(
    awaitables: Iterable[Awaitable[_]],
    period: Duration
  ): Unit = {
    if (awaitables.nonEmpty) {
      info(s"Awaiting ${awaitables.size} awaitables: \n${awaitables
        .map(_.getClass.getName)
        .mkString("\n")}")
      // Exit if ANY Awaitable is ready.
      val latch = new CountDownLatch(1)
      val task = DefaultTimer.schedule(period) {
        if (awaitables.exists(Await.isReady)) {
          awaitables.foreach { a: Awaitable[_] =>
            if (Await.isReady(a)) warn(s"${a.getClass.getName} awaitable has exited.")
          }
          latch.countDown()
        }
      }

      // We don't set a timeout because it should already be ready.
      latch.await()
      Await.result(task.close(), period) // wait at most the period for the timer task to close
    }
  }
}
