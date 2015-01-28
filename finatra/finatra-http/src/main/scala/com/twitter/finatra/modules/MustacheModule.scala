package com.twitter.finatra.modules

import com.github.mustachejava.{DefaultMustacheFactory, MustacheFactory}
import com.google.inject.Provides
import com.twitter.finatra.guice.GuiceModule
import com.twitter.mustache.ScalaObjectHandler
import javax.inject.Singleton

object MustacheModule extends GuiceModule {

  @Provides
  @Singleton
  def provideMustacheFactory: MustacheFactory = {
    val factory = new DefaultMustacheFactory("templates")
    factory.setObjectHandler(new ScalaObjectHandler)
    factory
  }
}
