package com.twitter.finatra.example;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Singleton;

@Singleton
public
class TestQueue extends Queue {

  public final AtomicInteger addCounter;

  public TestQueue() {
    super(Integer.MAX_VALUE);
    this.addCounter = new AtomicInteger();
  }

  @Override
  public boolean add(String value) {
    addCounter.incrementAndGet();
    return super.add(value);
  }
}
