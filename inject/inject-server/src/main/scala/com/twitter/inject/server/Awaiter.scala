package com.twitter.inject.server

import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Await, Awaitable, Duration, Future}
import java.util.concurrent.CountDownLatch

private[server] object Awaiter {

  /**
   * Awaits for ''any'' [[Awaitable]] to be ready (exit) and will then exit. This is
   * different than [[Await.all]] which only exits once ''all'' Awaitables are ready.
   * @param awaitables the list of [[Awaitable]]s to await.
   * @param period the interval on which to check each [[Awaitable.isReady]]
   */
  def any(
    awaitables: Iterable[Awaitable[_]],
    period: Duration
  ): Future[Unit] = {
    // exit if any Awaitable is ready
    val latch = new CountDownLatch(1)
    val task = DefaultTimer.schedule(period) {
      if (awaitables.exists(Await.isReady))
        latch.countDown()
    }

    // we don't set a timeout because it should already be ready
    latch.await()
    task.close()
  }
}
