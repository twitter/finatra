package com.twitter.finatra.sample

import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Singleton

/**
 * A specific concurrent queue implementation.
 */
@Singleton
class Queue {

  private[this] val underlying: ConcurrentLinkedQueue[String] = new ConcurrentLinkedQueue[String]()

  def poll: Option[String] = {
    Option(underlying.poll())
  }

  def add(value: String): Boolean = {
    underlying.add(value)
  }

  def size(): Int = {
    underlying.size()
  }
}
