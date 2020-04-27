package com.twitter.finatra.example

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.Logging
import com.twitter.util._
import javax.inject.{Inject, Singleton}

@Singleton
class Publisher @Inject()(queue: Queue)
    extends Closable
    with Awaitable[Unit]
    with Logging {

  private[this] val Pool: ExecutorServiceFuturePool = FuturePools.fixedPool("PublisherPool", 1)

  private[this] val stringsToPub: Set[String] =
    Set("Apple", "Banana", "Carrot", "Eggplant", "Fig")

  override def close(deadline: Time): Future[Unit] = {
    info("Publisher has finished writing to queue.")
    Future(Pool.executor.shutdown())
  }

  private[this] def publish(): Unit = {
    for (str <- stringsToPub) {
      info(s"Adding $str to the queue.")
      queue.add(str)
      info("Current values in queue: " + queue.mkString(",") + ". Depth: (" + queue.size + ").")
    }
  }

  // Implements Awaitable interface, but isReady always returns false
  // to prevent the Publisher from exiting once awaited on by the server
  override def ready(timeout: Duration)(implicit permit: Awaitable.CanAwait): Publisher.this.type =
    throw new TimeoutException(timeout.toString)

  override def result(timeout: Duration)(implicit permit: Awaitable.CanAwait): Unit =
    throw new TimeoutException(timeout.toString)

  override def isReady(implicit permit: Awaitable.CanAwait): Boolean = false

  def start(): Unit = Pool {
    // start now, publish every 10 minutes
    DefaultTimer.schedule(Time.now, 10.minutes)(publish())
  }
}
