package com.twitter.inject

import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.util.ExecutorServiceFuturePool
import java.util.concurrent.Executors

object PoolUtils {

  def newUnboundedPool(name: String): ExecutorServiceFuturePool = {
    new ExecutorServiceFuturePool(Executors.newCachedThreadPool(new NamedPoolThreadFactory(name)))
  }

  def newFixedPool(name: String, size: Int = 1): ExecutorServiceFuturePool = {
    new ExecutorServiceFuturePool(
      Executors.newFixedThreadPool(size, new NamedPoolThreadFactory(name))
    )
  }
}
