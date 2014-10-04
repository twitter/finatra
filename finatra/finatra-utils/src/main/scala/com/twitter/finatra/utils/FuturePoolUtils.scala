package com.twitter.finatra.utils

import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.util.{ExecutorServiceFuturePool, FuturePool}
import java.util.concurrent.Executors

object FuturePoolUtils {

  def fixedPool(name: String, size: Int): FuturePool = {
    new ExecutorServiceFuturePool(
      Executors.newFixedThreadPool(size,
        new NamedPoolThreadFactory(name)))
  }
}
