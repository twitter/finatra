package com.twitter.tiny.modules

import com.twitter.inject.TwitterModule
import com.twitter.tiny.services.UrlShortenerService
import com.twitter.tiny.services.impl.RedisUrlShortenerService

object ServicesModule extends TwitterModule {

  override val modules = Seq(JedisClientModule)

  override def configure: Unit = {
    bind[UrlShortenerService].to[RedisUrlShortenerService]
  }
}
