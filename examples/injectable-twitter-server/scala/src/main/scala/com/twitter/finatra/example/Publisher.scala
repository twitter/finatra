package com.twitter.finatra.example

import com.twitter.conversions.DurationOps._
import com.twitter.util._
import com.twitter.util.logging.Logging
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private object Publisher {
  val StringsToPub: Set[String] =
    Set("Apple", "Banana", "Carrot", "Eggplant", "Fig")
}

@Singleton
class Publisher @Inject() (queue: Queue) extends ClosableOnce with CloseOnceAwaitably with Logging {
  import Publisher._

  private[this] val PoolTimer: ScheduledThreadPoolTimer =
    new ScheduledThreadPoolTimer(1, "PublisherPool", false)
  private[this] val task: AtomicReference[TimerTask] = new AtomicReference[TimerTask](NullTimerTask)

  def closeOnce(deadline: Time): Future[Unit] = {
    task.get() match {
      case NullTimerTask =>
        info("Publisher closed.")
        Future.Done
      case task: TimerTask =>
        info("Publisher has finished writing to queue.")
        task.close(deadline)
    }
  }

  def start(): Unit = {
    info("Starting Publisher")
    task.compareAndSet(
      NullTimerTask, // should be null in order to start
      PoolTimer.schedule(Time.now, 10.minutes)(publish())
    )
  }

  private[this] def publish(): Unit = {
    info("Publisher#publish()")
    for (str <- StringsToPub) {
      info(s"Adding $str to the queue.")
      queue.add(str)
      info("Current values in queue: " + queue.mkString(",") + ". Depth: (" + queue.size + ").")
    }
  }
}
