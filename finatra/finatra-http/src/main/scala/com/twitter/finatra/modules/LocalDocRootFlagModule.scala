package com.twitter.finatra.modules

import com.github.mustachejava.{DefaultMustacheFactory, MustacheFactory}
import com.google.inject.Provides
import com.twitter.finatra.guice.GuiceModule
import com.twitter.mustache.ScalaObjectHandler
import javax.inject.Singleton

object LocalDocRootFlagModule extends GuiceModule {

  flag(
    "local.doc.root",
    "src/main/webapp/",
    "File serving directory when 'env' system property set to 'dev'")
}
