package com.twitter.finatra.http.modules

import com.github.mustachejava.{DefaultMustacheFactory, MustacheFactory}
import com.google.inject.Provides
import com.twitter.finatra.http.internal.marshalling.mustache.ScalaObjectHandler
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

object MustacheModule extends TwitterModule {

  private val templatesDir = flag("mustache.templates.dir", "templates", "templates resource directory")

  @Provides
  @Singleton
  def provideMustacheFactory: MustacheFactory = {
    val factory = new DefaultMustacheFactory(templatesDir())
    factory.setObjectHandler(new ScalaObjectHandler)
    factory
  }
}
