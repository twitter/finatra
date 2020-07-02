package com.twitter.finatra.example;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;

@Singleton
public
class TestQueue extends Queue {

  public final AtomicInteger addCounter;
  public final CountDownLatch firstWriteLatch;

  public TestQueue() {
    super(Integer.MAX_VALUE);
    this.addCounter = new AtomicInteger();
    this.firstWriteLatch = new CountDownLatch(1);
  }

  @Override
  public boolean add(String value) {
    addCounter.incrementAndGet();
    firstWriteLatch.countDown();
    return super.add(value);
  }
}
