package com.twitter.inject.thrift.modules

import com.twitter.inject.TwitterModule
import com.twitter.inject.thrift.AndThenService
import com.twitter.inject.thrift.internal.DefaultAndThenServiceImpl

@deprecated("No replacement.", "2022-03-03")
object AndThenServiceModule extends TwitterModule {

  override def configure(): Unit = {
    bind[AndThenService].to[DefaultAndThenServiceImpl]
  }

  /**  Java-friendly way to access this module as a singleton instance */
  def get(): this.type = this
}
