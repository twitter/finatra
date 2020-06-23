package com.twitter.finatra.kafka.utils

import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.util.FuturePool
import java.util.concurrent.{
  ArrayBlockingQueue,
  BlockingQueue,
  LinkedBlockingQueue,
  ThreadPoolExecutor,
  TimeUnit
}

object FuturePoolsHelper {

  private def fixedThreadPool[T](
    name: String,
    maxThreads: Int,
    workQueue: BlockingQueue[Runnable]
  ): KafkaFuturePool = {
    val threadFactory = new NamedPoolThreadFactory(name, makeDaemons = true)

    // this is the same as Executors.newFixedThreadPool(), but with
    // a monitored work queue
    val executorService =
      new ThreadPoolExecutor(
        maxThreads,
        maxThreads, // ignored by ThreadPoolExecutor when an unbounded queue is provided
        0,
        TimeUnit.MILLISECONDS,
        workQueue,
        threadFactory)

    KafkaFuturePool(FuturePool.interruptible(executorService), () => workQueue.size())
  }

  def boundedFixedThreadPool[T](
    name: String,
    maxThreads: Int,
    maxWorkQueueSize: Int
  ): KafkaFuturePool =
    fixedThreadPool(name, maxThreads, new ArrayBlockingQueue[Runnable](maxWorkQueueSize))

  def unboundedFixedThreadPool[T](
    name: String,
    maxThreads: Int
  ): KafkaFuturePool =
    fixedThreadPool(name, maxThreads, new LinkedBlockingQueue[Runnable])
}

case class KafkaFuturePool(pool: FuturePool, getQueueSize: () => Int)
