package com.twitter.finatra.modules

import com.github.mustachejava.{DefaultMustacheFactory, MustacheFactory}
import com.google.inject.Provides
import com.twitter.finatra.guice.GuiceModule
import com.twitter.mustache.ScalaObjectHandler
import javax.inject.Singleton

object MustacheModule extends GuiceModule {

  private val templatesDir = flag("mustache.templates.dir", "templates", "templates resource directory")

  @Provides
  @Singleton
  def provideMustacheFactory: MustacheFactory = {
    val factory = new DefaultMustacheFactory(templatesDir())
    factory.setObjectHandler(new ScalaObjectHandler)
    factory
  }
}
