package com.twitter.finatra.example

import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Singleton
import scala.collection.JavaConverters._

/**
 * A specific concurrent queue implementation.
 */
@Singleton
class Queue extends Iterable[String] {

  private[this] val underlying: ConcurrentLinkedQueue[String] = new ConcurrentLinkedQueue[String]()

  def poll: Option[String] = {
    Option(underlying.poll())
  }

  def add(value: String): Boolean = {
    underlying.add(value)
  }

  override def size: Int = {
    underlying.size()
  }

  override def iterator: Iterator[String] = underlying.iterator().asScala
}
