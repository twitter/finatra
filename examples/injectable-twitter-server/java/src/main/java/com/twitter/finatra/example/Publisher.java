package com.twitter.finatra.example;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

import scala.runtime.BoxedUnit;

import com.google.common.collect.ImmutableSet;

import com.twitter.finagle.util.DefaultTimer;
import com.twitter.finatra.utils.FuturePools;
import com.twitter.util.Awaitable;
import com.twitter.util.Closable;
import com.twitter.util.Duration;
import com.twitter.util.ExecutorServiceFuturePool;
import com.twitter.util.Function0;
import com.twitter.util.Future;
import com.twitter.util.Time;
import com.twitter.util.TimeoutException;
import com.twitter.util.logging.Logger;

@Singleton
public class Publisher implements Closable, Awaitable<Void> {

  private final Logger logger = Logger.apply(Publisher.class);

  private final Set<String> stringsToPub =
      ImmutableSet.of("Apple", "Banana", "Carrot", "Eggplant", "Fig");

  private final ExecutorServiceFuturePool pool =
      FuturePools.fixedPool("PublisherPool", 1);

  private final Queue queue;

  @Inject
  public Publisher(Queue queue) {
    this.queue = queue;
  }

  @Override
  public Future<BoxedUnit> close(Time deadline) {
    logger.info("Publisher has finished writing to queue.");
    return Future.apply(new Function0<BoxedUnit>() {
      @Override
      public BoxedUnit apply() {
        pool.executor().shutdown();
        return BoxedUnit.UNIT;
      }
    });
  }

  private BoxedUnit publish() {
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
    // start now, publish every 10 minutes
    DefaultTimer.schedule(
        Time.now(),
        Duration.fromMinutes(10),
        new Function0<BoxedUnit>() {
          @Override
          public BoxedUnit apply() {
            return publish();
          }
        });
  }

  // Implements Awaitable interface, but isReady always returns false
  // to prevent the Publisher from exiting once awaited on by the server
  @Override
  public Publisher ready(Duration timeout, CanAwait permit) throws TimeoutException {
    throw new TimeoutException(timeout.toString());
  }

  @Override
  public Void result(Duration timeout, CanAwait permit) throws Exception {
    throw new TimeoutException(timeout.toString());
  }

  @Override
  public boolean isReady(CanAwait permit) {
    return false;
  }

  @Override
  public Future<BoxedUnit> close() {
    return this.close(Time.now());
  }

  @Override
  public Future<BoxedUnit> close(Duration after) {
    return this.close(Time.now().plus(after));
  }
}
