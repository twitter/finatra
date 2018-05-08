package com.twitter.finatra.sample

import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.Logging
import com.twitter.util._
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.{Inject, Singleton}

@Singleton
class Subscriber @Inject()(queue: Queue)
    extends Closable
    with Awaitable[Unit]
    with Logging {

  val numRead: AtomicInteger = new AtomicInteger(0)

  private val Pool: ExecutorServiceFuturePool = FuturePools.fixedPool("SubscriberPool", 1)

  override def close(deadline: Time): Future[Unit] = {
    info("Subscriber has stopped reading from queue.")
    Future(Pool.executor.shutdown())
  }

  // Implements Awaitable interface, but isReady always returns false
  // to prevent the Subscriber from exiting once awaited on by the server
  override def ready(timeout: Duration)(implicit permit: Awaitable.CanAwait): Subscriber.this.type =
    throw new TimeoutException(timeout.toString)

  override def result(timeout: Duration)(implicit permit: Awaitable.CanAwait): Unit =
    throw new TimeoutException(timeout.toString)

  override def isReady(implicit permit: Awaitable.CanAwait): Boolean = false

  def start(): Unit = Pool {
    while (true) {
      val str: Option[String] = queue.poll

      str match {
        case Some(value) =>
          info(s"Reading $str from the queue.")
          numRead.incrementAndGet()
        case _ =>
          // do nothing
      }
    }
  }
}
