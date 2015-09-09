package com.twitter.tiny.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import com.twitter.tiny.services.{Counter, UrlShortenerService}
import com.twitter.tiny.services.impl.RedisUrlShortenerService
import redis.clients.jedis.{Jedis => JedisClient}

object ServicesModule extends TwitterModule {

  override val modules = Seq(JedisClientModule)

  override def configure: Unit = {
    bind[UrlShortenerService].to[RedisUrlShortenerService]
  }
}
