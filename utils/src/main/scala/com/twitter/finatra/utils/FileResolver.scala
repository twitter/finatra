package com.twitter.finatra.utils

import com.twitter.inject.conversions.boolean._
import com.twitter.util.logging.Logger
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.activation.MimetypesFileTypeMap

object FileResolver {

  /** Creates a new [[FileResolver]] with the given local document root. */
  def newLocalResolver(root: String): FileResolver =
    new FileResolver(localDocRoot = root, docRoot = "")

  /** Creates a new [[FileResolver]] with the given document root. */
  def newResolver(root: String): FileResolver =
    new FileResolver(localDocRoot = "", docRoot = root)

  private val logger: Logger = Logger(FileResolver.getClass)
}

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
 */
class FileResolver(localDocRoot: String, docRoot: String) {
  import FileResolver._

  // assertions -- cannot have both doc roots set
  if (localDocRoot.nonEmpty && docRoot.nonEmpty) {
    throw new java.lang.AssertionError(
      "assertion failed: Cannot set both -local.doc.root and -doc.root flags."
    )
  }

  private val extMap = new MimetypesFileTypeMap()
  private val localFileMode = localDocRoot.nonEmpty.onTrue {
    logger.info("Local file mode enabled")
  }
  private val actualDocRoot = if (docRoot.startsWith("/")) docRoot else "/" + docRoot

  /* Public */

  def getInputStream(path: String): Option[InputStream] = {
    assert(path.startsWith("/"))
    if (isDirectory(path)) {
      None
    } else if (localFileMode) {
      getLocalFileInputStream(path)
    } else {
      getClasspathInputStream(path)
    }
  }

  def exists(path: String): Boolean = {
    assert(path.startsWith("/"))
    if (isDirectory(path)) {
      false
    } else if (localFileMode) {
      // try absolute path first, then under local.doc.root
      Files.exists(Paths.get(path)) || Files.exists(Paths.get(localDocRoot, path))
    } else {
      getClasspathInputStream(path).isDefined
    }
  }

  def getContentType(file: String): String =
    extMap.getContentType(dottedFileExtension(file))

  def getFileExtension(filename: String): String = {
    val lastSeparator = getLastSeparatorIndex(filename)
    val extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR)
    if (lastSeparator >= extensionPos) "" else filename.substring(extensionPos)
  }

  /* Private */

  private[this] final val UNIX_SEPARATOR = '/'
  private[this] final val WINDOWS_SEPARATOR = '\\'
  private[this] final val EXTENSION_SEPARATOR = '.'

  private[this] def getLastSeparatorIndex(filename: String): Int = {
    val lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR)
    val lastWindowPos = filename.lastIndexOf(WINDOWS_SEPARATOR)
    if (lastUnixPos > lastWindowPos) lastUnixPos else lastWindowPos
  }

  private[this] def isDirectory(path: String): Boolean = path.endsWith("/")

  private[this] def getClasspathInputStream(path: String): Option[InputStream] = {
    val actualPath = if (!docRoot.isEmpty) s"$actualDocRoot$path" else path
    for {
      is <- Option(getClass.getResourceAsStream(actualPath))
      bis = new BufferedInputStream(is)
      if bis.available > 0
    } yield bis
  }

  private[this] def getLocalFileInputStream(path: String): Option[InputStream] = {
    // try absolute path first, then under local DocumentRoot
    val file = if (Files.exists(Paths.get(path))) {
      new File(path)
    } else {
      new File(localDocRoot, path)
    }

    if (file.exists) {
      Option(new BufferedInputStream(new FileInputStream(file)))
    } else None
  }

  private[this] def dottedFileExtension(uri: String) = s".${getFileExtension(uri)}"
}
