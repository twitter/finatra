package com.twitter.finatra.mustache.modules

import com.github.mustachejava.MustacheFactory
import com.google.inject.{Module, Provides}
import com.twitter.finatra.modules.{FileResolverFlags, FileResolverModule}
import com.twitter.finatra.mustache.marshalling.MustacheFactoryBuilder
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.Flag
import javax.inject.Singleton

object MustacheFlags {

  /**
   * The [[com.twitter.app.Flag]] bound can be accessed via injection
   * by using
   * {{{
   *   @Flag(MustacheFlags.TemplatesDirectory) templatesDirectory
   * }}}
   */
  final val TemplatesDirectory = "mustache.templates.dir"
}

/**
 * Note: This module depends on the [[com.twitter.finatra.modules.FileResolverModule]] which
 * defines flags configuring the document root for resolving files. When the `local.doc.root`
 * is set to a non-empty value this will force the [[MustacheFactoryModule]] to NOT cache templates
 * reloading a template every time it is rendered. Generally, the `local.doc.root` flag
 * should only be set to a non-empty value when doing local development and empty otherwise.
 */
object MustacheFactoryModule extends TwitterModule {

  override def modules: Seq[Module] =
    Seq(FileResolverModule, MustacheFlagsModule)

  @Provides
  @Singleton
  private final def provideMustacheFactory(
    fileResolver: FileResolver,
    @Flag(MustacheFlags.TemplatesDirectory) templatesDirectory: String,
    @Flag(FileResolverFlags.LocalDocRoot) localDocRoot: String
  ): MustacheFactory = {
    // templates are cached only if there is no local.doc.root
    MustacheFactoryBuilder.newFactory(
      fileResolver,
      templatesDirectory,
      cacheTemplates = localDocRoot.isEmpty)
  }
}
