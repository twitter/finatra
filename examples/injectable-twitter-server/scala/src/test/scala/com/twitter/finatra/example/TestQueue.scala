package com.twitter.finatra.example

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

@Singleton
class TestQueue extends Queue(Integer.MAX_VALUE) {
  val addCounter: AtomicInteger = new AtomicInteger(0)

  override def add(value: String): Boolean = {
    addCounter.incrementAndGet()
    super.add(value)
  }
}
