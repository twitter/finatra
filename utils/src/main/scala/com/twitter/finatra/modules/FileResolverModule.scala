package com.twitter.finatra.modules

import com.google.inject.Provides
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.Flag
import javax.inject.Singleton

object FileResolverFlags {

  /**
   * Denotes the flag name representing the flag input configuring the
   * directory or namespace for classpath resources. The flag value can be
   * accessed via injection, e.g.,
   *
   * @example
   * {{{
   *  class MyService @Inject()(
   *      @Flag(FileResolverFlags.DocRoot) docRoot: String
   *  )
   * }}}
   * @note It is an error set both the [[LocalDocRoot]] and [[DocRoot]] flags.
   */
  final val DocRoot = "doc.root"

  /**
   * Denotes the flag name representing the flag input configuring the
   * directory for local file serving. The flag value can be accessed via
   * injection, e.g.,
   *
   * @example
   * {{{
   *  class MyService @Inject()(
   *      @Flag(FileResolverFlags.LocalDocRoot) localDocRoot: String
   *  )
   * }}}
   *
   * @note The `local.doc.root` flag SHOULD only be set during local development.
   * @note additionally, it is an error set both the [[LocalDocRoot]] and [[DocRoot]] flags.
   */
  final val LocalDocRoot = "local.doc.root"
}

/**
 * A [[com.twitter.inject.TwitterModule]] which provides a configured
 * [[com.twitter.finatra.utils.FileResolver]].
 */
object FileResolverModule extends TwitterModule {

  // Only one of these flags should ever be set to a non-empty string as
  // these flags are mutually exclusive. Setting both will result in error.
  flag(FileResolverFlags.DocRoot, "", "File serving directory/namespace for classpath resources")
  flag(FileResolverFlags.LocalDocRoot, "", "File serving directory for local development")

  @Provides
  @Singleton
  private final def provideFileResolver(
    @Flag(FileResolverFlags.DocRoot) documentRoot: String,
    @Flag(FileResolverFlags.LocalDocRoot) localDocumentRoot: String
  ): FileResolver = new FileResolver(localDocumentRoot, documentRoot)

  /**  Java-friendly way to access this module as a singleton instance */
  def get(): this.type = this
}
