package com.twitter.tiny.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import com.twitter.tiny.services.{Counter, UrlShortenerService}
import com.twitter.tiny.services.impl.RedisUrlShortenerService
import redis.clients.jedis.{Jedis => JedisClient}

object ServicesModule extends TwitterModule {

  override val modules = Seq(new JedisClientModule)

  @Singleton
  @Provides
  def provideUrlShortnerService(
    jedisClient: JedisClient): UrlShortenerService = {
    new RedisUrlShortenerService(
      jedisClient,
      new Counter(jedisClient))
  }
}
