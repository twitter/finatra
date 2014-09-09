package com.twitter.finatra.twitterserver.modules

import com.github.mustachejava.{DefaultMustacheFactory, MustacheFactory}
import com.google.inject.Provides
import com.twitter.finatra.guice.GuiceModule
import com.twitter.mustache.TwitterObjectHandler
import javax.inject.Singleton

object MustacheModule extends GuiceModule {

  @Provides
  @Singleton
  def provideMustacheFactory: MustacheFactory = {
    val factory = new DefaultMustacheFactory("templates")
    factory.setObjectHandler(new TwitterObjectHandler)
    factory
  }
}
