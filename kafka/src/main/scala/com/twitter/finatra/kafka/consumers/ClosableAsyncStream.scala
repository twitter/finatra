package com.twitter.finatra.kafka.consumers

import com.twitter.concurrent.AsyncStream
import com.twitter.util.Closable

trait ClosableAsyncStream[T] extends Closable {
  def asyncStream(): AsyncStream[T]
}
