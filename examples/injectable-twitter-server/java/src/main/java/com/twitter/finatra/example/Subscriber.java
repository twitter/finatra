package com.twitter.finatra.example;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;

import scala.runtime.BoxedUnit;

import com.twitter.inject.annotations.Flag;
import com.twitter.util.AbstractCloseAwaitably;
import com.twitter.util.Duration;
import com.twitter.util.Function0;
import com.twitter.util.Future;
import com.twitter.util.NullTimerTask$;
import com.twitter.util.ScheduledThreadPoolTimer;
import com.twitter.util.Time;
import com.twitter.util.TimerTask;
import com.twitter.util.logging.Logger;

@Singleton
public class Subscriber extends AbstractCloseAwaitably {
  private final Logger logger = Logger.apply(Subscriber.class);

  private final Queue queue;
  private final int maxRead;
  private final AtomicInteger numRead;
  private final ScheduledThreadPoolTimer timer;
  private final AtomicReference<TimerTask> task;

  @Inject
  public Subscriber(
      Queue queue,
      @Flag("subscriber.max.read") int maxRead) {
    this.queue = queue;
    this.maxRead = maxRead;
    this.numRead = new AtomicInteger();
    this.timer = new ScheduledThreadPoolTimer(1, "SubscriberPool", false);
    this.task = new AtomicReference<>(NullTimerTask$.MODULE$);
  }

  @Override
  public Future<BoxedUnit> onClose(Time deadline) {
    TimerTask currentTask = task.get();
    if (!NullTimerTask$.MODULE$.equals(currentTask)) {
      logger.info("Subscriber has stopped reading from queue.");
      return currentTask.close(deadline);
    } else {
      logger.info("Subscriber closed.");
      return Future.Done();
    }
  }

  public int readCount() {
    return numRead.get();
  }

  /**
   * Begin the scheduled task of reading items from the queue.
   */
  public void start() {
    logger.info("Starting Subscriber");
    task.compareAndSet(
        NullTimerTask$.MODULE$,
        timer.schedule(
          Time.now().plus(Duration.fromSeconds(2)),
          Duration.fromSeconds(1),
          new Function0<BoxedUnit>() {
          @Override
          public BoxedUnit apply() {
            return read();
          }
        })
    );
  }

  private BoxedUnit read() {
    logger.info("Subscriber#read()");
    Optional<String> str = queue.poll();
    str.ifPresent(s -> {
      numRead.incrementAndGet();
      logger.info("Reading " + s + " value from the queue. Read: " + numRead.get() + ".");
    });
    return BoxedUnit.UNIT;
  }

  @Override
  public boolean isReady(CanAwait permit) {
    return numRead.get() >= this.maxRead;
  }
}
