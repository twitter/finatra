package com.twitter.finatra.mustache.marshalling

import com.github.mustachejava.{DefaultMustacheFactory, MustacheFactory, Mustache => JMustache}
import com.twitter.finatra.utils.FileResolver

object MustacheFactoryBuilder {

  /**
   * Constructs a new [[com.github.mustachejava.MustacheFactory]] based on the given inputs.
   *
   * @param fileResolver a [[com.twitter.finatra.utils.FileResolver]] instance.
   * @param templatesDirectory the directory from where to load Mustache template files.
   * @param cacheTemplates if loaded templates should be cached or reloaded from the local
   *                       filesystem every time.
   *
   * @note Generally `cacheTemplates` should only be set to "false" during development.
   * @return a new [[com.github.mustachejava.MustacheFactory]].
   */
  def newFactory(
    fileResolver: FileResolver,
    templatesDirectory: String,
    cacheTemplates: Boolean
  ): MustacheFactory = new DefaultMustacheFactory(templatesDirectory) {
    setObjectHandler(new ScalaObjectHandler)
    override def compile(name: String): JMustache = {
      if (cacheTemplates) {
        super.compile(name)
      } else {
        new LocalFilesystemMustacheFactoryImpl(
          templatesDirectory,
          fileResolver
        ).compile(name)
      }
    }
  }

  /**
   * A local filesystem-only MustacheFactory. Uses the [[com.twitter.finatra.utils.FileResolver]]
   * for resolution and does not internally cache templates.
   */
  private final class LocalFilesystemMustacheFactoryImpl(
    override val templatesDirectory: String,
    override val resolver: FileResolver)
      extends DefaultMustacheFactory
      with LocalFilesystemMustacheFactory
}
