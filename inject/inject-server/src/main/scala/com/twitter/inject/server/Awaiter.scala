package com.twitter.inject.server

import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.Logging
import com.twitter.util.{Await, Awaitable, Duration, Future}
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
  ): Future[Unit] = {
    info(s"Awaiting ${awaitables.size} awaitables: \n${awaitables.map(_.getClass.getName).mkString("\n")}")
    // Exit if ANY Awaitable is ready.
    val latch = new CountDownLatch(1)
    val task = DefaultTimer.schedule(period) {
      if (awaitables.exists(Await.isReady)) {
        awaitables.foreach { a: Awaitable[_] =>
          // It is not expected that these Awaitables will exit here, but rather that the server
          // will instead close thus triggering the Awaitables to also close. Therefore a "ready"
          // Awaitable here is considered an error and logged as such.
          if (Await.isReady(a)) error(s"${a.getClass.getName} awaitable has exited.")
        }
        latch.countDown()
      }
    }

    // We don't set a timeout because it should already be ready.
    latch.await()
    task.close()
  }
}
