package com.twitter.inject.thrift.modules

import com.twitter.inject.TwitterModule
import com.twitter.inject.thrift.AndThenService
import com.twitter.inject.thrift.internal.DefaultAndThenServiceImpl

object AndThenServiceModule extends TwitterModule {

  override def configure: Unit = {
    bindSingleton[AndThenService].to[DefaultAndThenServiceImpl]
  }
}