package com.twitter.finatra.utils

import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import com.twitter.inject.conversions.boolean._
import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import javax.activation.MimetypesFileTypeMap
import javax.inject.{Inject, Singleton}

/**
 * Non-optimized file resolver. The resolver takes in two possible parameters,
 * a 'localDocRoot' or a 'docRoot'. Note that these two parameters are MUTUALLY
 * EXCLUSIVE, e.g., only one or the other should ever be set to a non-empty value.
 *
 * If neither is set to a non-empty value, the default behaviour is to load resources
 * from the classpath from a root of "/".
 *
 * NOTE: Not for true external production use serving static resources (Use a real
 * static file server!) as this class provides no optimizations or caching.
 *
 * @param localDocRoot - File serving directory for local development only.
 * @param docRoot - File serving directory/namespace for classpath resources.
 * @see com.twitter.finatra.http.modules.DocRootModule
 */
@Singleton
class FileResolver @Inject()(
  @Flag("local.doc.root") localDocRoot: String,
  @Flag("doc.root") docRoot: String
) extends Logging {

  // assertions -- cannot have both doc roots set
  if (localDocRoot.nonEmpty && docRoot.nonEmpty) {
    throw new java.lang.AssertionError(
      "assertion failed: Cannot set both -local.doc.root and -doc.root flags."
    )
  }

  private val extMap = new MimetypesFileTypeMap()
  private val localFileMode = localDocRoot.nonEmpty.onTrue {
    info("Local file mode enabled")
  }
  private val actualDocRoot = if (docRoot.startsWith("/")) docRoot else "/" + docRoot

  /* Public */

  def getInputStream(path: String): Option[InputStream] = {
    assert(path.startsWith("/"))
    if (isDirectory(path))
      None
    else if (localFileMode)
      getLocalFileInputStream(path)
    else
      getClasspathInputStream(path)
  }

  def exists(path: String): Boolean = {
    assert(path.startsWith("/"))
    if (isDirectory(path))
      false
    else if (localFileMode)
      // try absolute path first, then under local.doc.root
      new File(path).exists || new File(localDocRoot, path).exists
    else
      getClasspathInputStream(path).isDefined
  }

  def getContentType(file: String): String =
    extMap.getContentType(dottedFileExtension(file))

  def getFileExtension(filename: String): String = {
    val lastSeparator = getLastSeperatorIndex(filename)
    val extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR)
    if (lastSeparator >= extensionPos) "" else filename.substring(extensionPos)
  }

  /* Private */

  private final val UNIX_SEPARATOR = '/'
  private final val WINDOWS_SEPARATOR = '\\'
  private final val EXTENSION_SEPARATOR = '.'

  private def getLastSeperatorIndex(filename: String): Int = {
    val lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR)
    val lastWindowPos = filename.lastIndexOf(WINDOWS_SEPARATOR)
    if (lastUnixPos > lastWindowPos) lastUnixPos else lastWindowPos
  }

  private def isDirectory(path: String): Boolean = path.endsWith("/")

  private def getClasspathInputStream(path: String): Option[InputStream] = {
    val actualPath = if (!docRoot.isEmpty) s"$actualDocRoot$path" else path
    for {
      is <- Option(getClass.getResourceAsStream(actualPath))
      bis = new BufferedInputStream(is)
      if bis.available > 0
    } yield bis
  }

  private def getLocalFileInputStream(path: String): Option[InputStream] = {
    // try absolute path first, then under local.doc.root
    val file =
      if (new File(path).exists)
        new File(path)
      else
        new File(localDocRoot, path)

    if (file.exists)
      Option(new BufferedInputStream(new FileInputStream(file)))
    else
      None
  }

  private def dottedFileExtension(uri: String) = '.' + getFileExtension(uri)
}
