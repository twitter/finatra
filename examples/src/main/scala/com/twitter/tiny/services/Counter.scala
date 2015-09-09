package com.twitter.tiny.services

import com.twitter.tiny.services.Counter._
import redis.clients.jedis.{Jedis => JedisClient}

object Counter {
  val InitialValue = 10000000L; // (ten million)
  val CounterKey = "tiny-url:counter"
}

class Counter (client: JedisClient) {
  def next: Long = {
    val current: Long = Option(client.get(CounterKey)) match {
      case Some(value) =>
        value.toLong
      case None =>
        InitialValue
    }
    val nextValue = current + 1
    client.set(CounterKey, nextValue.toString)
    nextValue
  }
}
