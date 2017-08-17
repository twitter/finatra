package com.twitter.tiny.services.impl

import javax.inject.Inject

import com.twitter.inject.Logging
import com.twitter.tiny.services.{Counter, EncodingRadix, UrlShortenerService}
import com.twitter.tiny.services.impl.RedisUrlShortenerService._
import redis.clients.jedis.{Jedis => JedisClient}

object RedisUrlShortenerService {
  private[services] val KeyPrefix = "url-"
}

class RedisUrlShortenerService @Inject()(client: JedisClient, counter: Counter)
    extends UrlShortenerService
    with Logging {

  /**
   * Maps the given URL to a 32-radix integer based on the next value in
   * the counter and returns that key to be used as the path for resolving
   * the shortened URL on lookup.
   * @param url - the URL to be shortened.
   * @return the 32-radix integer representation of the counter mapped to the URL.
   */
  def create(url: java.net.URL): String = {
    info(s"Creating shortened URL for: ${url.toString}")
    // use the next value of the counter as the key in cache
    val nextValue = counter.next
    client.set("%s%s".format(KeyPrefix, nextValue.toString), url.toString)
    java.lang.Long.toString(nextValue, EncodingRadix)
  }

  /**
   * Given a 32-radix integer as a String, find the url mapped to
   * it in Redis.
   * @param id the 32-radix integer as a String to use as the key
   * @return if found a Some(String) of the mapped URL, None if no value is found for the determined key
   */
  def get(id: String): Option[String] = {
    val value = java.lang.Long.valueOf(id, EncodingRadix)
    Option(client.get("%s%s".format(KeyPrefix, value.toString)))
  }
}
