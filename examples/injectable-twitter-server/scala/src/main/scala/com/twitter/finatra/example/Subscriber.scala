package com.twitter.finatra.example

import com.twitter.conversions.DurationOps._
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import com.twitter.util._
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import javax.inject.{Inject, Singleton}

@Singleton
class Subscriber @Inject() (
  queue: Queue,
  @Flag("subscriber.max.read") maxRead: Int)
    extends ClosableOnce
    with CloseOnceAwaitably
    with Logging {

  private[this] val numRead: AtomicInteger = new AtomicInteger(0)
  private[this] val PoolTimer: ScheduledThreadPoolTimer =
    new ScheduledThreadPoolTimer(1, "SubscriberPool", false)
  private[this] val task: AtomicReference[TimerTask] = new AtomicReference[TimerTask](NullTimerTask)

  def closeOnce(deadline: Time): Future[Unit] = {
    task.get() match {
      case NullTimerTask =>
        info("Subscriber closed.")
        Future.Done
      case task: TimerTask =>
        info("Subscriber has stopped reading from queue.")
        task.close(deadline)
    }
  }

  def readCount: Int = numRead.get()

  def start(): Unit = {
    info("Starting Subscriber.")
    task.compareAndSet(
      NullTimerTask, // should be null in order to start
      PoolTimer.schedule(Time.now.plus(2.seconds), 1.seconds)(read())
    )
  }

  private[this] def read(): Unit = {
    info("Subscriber#read()")
    queue.poll match {
      case Some(value) =>
        numRead.incrementAndGet()
        info("Reading " + value + " value from the queue. Read: " + numRead.get + ".")
      case _ =>
      // do nothing
    }
  }

  override def isReady(implicit permit: Awaitable.CanAwait): Boolean = numRead.get >= maxRead
}
