package com.twitter.finatra.example

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.util.DefaultTimer
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

  private[this] def read(): Unit = {
    val str: Option[String] = queue.poll
    str match {
      case Some(value) =>
        numRead.incrementAndGet()
        info("Reading " + value + " value from the queue. Read: " + numRead.get + ".")
      case _ =>
      // do nothing
    }
  }

  // Implements Awaitable interface, but isReady always returns false
  // to prevent the Subscriber from exiting once awaited on by the server
  override def ready(timeout: Duration)(implicit permit: Awaitable.CanAwait): Subscriber.this.type =
    throw new TimeoutException(timeout.toString)

  override def result(timeout: Duration)(implicit permit: Awaitable.CanAwait): Unit =
    throw new TimeoutException(timeout.toString)

  override def isReady(implicit permit: Awaitable.CanAwait): Boolean = false

  def start(): Unit = {
    DefaultTimer.schedule(Time.now.plus(5.seconds), 30.seconds)(read())
  }
}
