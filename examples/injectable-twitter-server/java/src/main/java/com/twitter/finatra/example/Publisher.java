package com.twitter.finatra.example;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;

import scala.runtime.BoxedUnit;

import com.google.common.collect.ImmutableSet;

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
public class Publisher extends AbstractCloseAwaitably {
  private final Logger logger = Logger.apply(Publisher.class);

  private final Set<String> stringsToPub =
      ImmutableSet.of("Apple", "Banana", "Carrot", "Eggplant", "Fig");

  private final Queue queue;
  private final ScheduledThreadPoolTimer timer;
  private final AtomicReference<TimerTask> task;

  @Inject
  public Publisher(Queue queue) {
    this.queue = queue;
    this.timer = new ScheduledThreadPoolTimer(1, "PublisherPool", false);
    this.task = new AtomicReference<>(NullTimerTask$.MODULE$);
  }

  @Override
  public Future<BoxedUnit> onClose(Time deadline) {
    TimerTask currentTask = task.get();
    if (!NullTimerTask$.MODULE$.equals(currentTask)) {
      logger.info("Publisher has finished writing to queue.");
      return currentTask.close(deadline);
    } else {
      logger.info("Publisher closed.");
      return Future.Done();
    }
  }

  private BoxedUnit publish() {
    logger.info("Publisher#publish()");
    stringsToPub.forEach(str -> {
      logger.info("Adding " + str + " to the queue.");
      queue.add(str);
      logger.info(
          "Current values in queue: " + mkString(queue) + ". Depth: (" + queue.size() + ").");
    });
    return BoxedUnit.UNIT;
  }

  private String mkString(Iterable<String> iter) {
    StringBuilder sb = new StringBuilder();
    iter.forEach(s -> {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(s);
    });
    return sb.toString();
  }

  /** Begin the scheduled task of writing to the queue */
  public void start() {
    logger.info("Starting Publisher");
      // start now, publish every 10 minutes
      task.compareAndSet(
          NullTimerTask$.MODULE$,
          timer.schedule(
            Time.now(),
            Duration.fromMinutes(10),
            new Function0<BoxedUnit>() {
            @Override
            public BoxedUnit apply() {
              return publish();
            }
          })
      );
  }
}
