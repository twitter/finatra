package com.twitter.finatra.mustache.marshalling

import com.github.mustachejava.{DefaultMustacheFactory, MustacheNotFoundException}
import com.twitter.finatra.utils.FileResolver
import java.io.{InputStreamReader, Reader}

trait LocalFilesystemMustacheFactory { self: DefaultMustacheFactory =>
  def templatesDirectory: String
  def resolver: FileResolver

  setObjectHandler(new ScalaObjectHandler)

  /**
   * Resolves templates given a [[com.twitter.finatra.utils.FileResolver]]. If the given
   * `resourceName` starts with a `/` it is considered an absolute path otherwise the
   * resource is considered to be at a path relative to the configured [[templatesDirectory]].
   * @param resourceName the path of the Mustache template to resolve.
   * @return a [[Reader]] to the Mustache template.
   *
   * @see [[com.github.mustachejava.MustacheFactory#getReader]]
   */
  override def getReader(resourceName: String): Reader = {
    // Relative paths are prefixed by the templates directory.
    val filePath = if (resourceName.startsWith("/")) {
      resourceName
    } else if (templatesDirectory.startsWith("/")) {
      s"$templatesDirectory/$resourceName"
    } else {
      s"/$templatesDirectory/$resourceName"
    }

    resolver
      .getInputStream(filePath)
      .map(new InputStreamReader(_))
      .getOrElse(
        throw new MustacheNotFoundException(resourceName)
      )
  }

}
