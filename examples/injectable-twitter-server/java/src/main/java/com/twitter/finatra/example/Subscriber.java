package com.twitter.finatra.example;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;

import scala.runtime.BoxedUnit;

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
public class Subscriber implements Closable, Awaitable<Void> {

  private final Logger logger = Logger.apply(Subscriber.class);

  private final Queue queue;
  private final AtomicInteger numRead;
  private final ExecutorServiceFuturePool pool;

  @Inject
  public Subscriber(Queue queue) {
    this.queue = queue;
    this.numRead = new AtomicInteger();
    this.pool = FuturePools.fixedPool("SubscriberPool", 1);
  }

  @Override
  public Future<BoxedUnit> close(Time deadline) {
    logger.info("Subscriber has stopped reading from queue.");
    return Future.apply(new Function0<BoxedUnit>() {
      @Override
      public BoxedUnit apply() {
        pool.executor().shutdown();
        return BoxedUnit.UNIT;
      }
    });
  }

  /**
   * Begin the scheduled task of reading items from the queue.
   */
  public void start() {
    DefaultTimer.schedule(
        Time.now().plus(Duration.fromSeconds(10)),
        Duration.fromSeconds(30),
        new Function0<BoxedUnit>() {
          @Override
          public BoxedUnit apply() {
            return read();
          }
        });
  }

  private BoxedUnit read() {
    Optional<String> str = queue.poll();
    str.ifPresent(s -> {
      numRead.incrementAndGet();
      logger.info("Reading " + s + " value from the queue. Read: " + numRead.get() + ".");
    });
    return BoxedUnit.UNIT;
  }

  // Implements Awaitable interface, but isReady always returns false
  // to prevent the Subscriber from exiting once awaited on by the server
  @Override
  public Awaitable<Void> ready(Duration timeout, CanAwait permit) throws TimeoutException {
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
