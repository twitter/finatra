package com.twitter.finatra.utils

/**
 * For Java usability.
 *
 * @note Scala users see [[com.twitter.finatra.utils.FileResolver]]
 */
object FileResolvers {

  /** Creates a new [[FileResolver]] with the given local document root. */
  def newLocalResolver(root: String): FileResolver = FileResolver.newLocalResolver(root)

  /** Creates a new [[FileResolver]] with the given document root. */
  def newResolver(root: String): FileResolver = FileResolver.newResolver(root)
}
