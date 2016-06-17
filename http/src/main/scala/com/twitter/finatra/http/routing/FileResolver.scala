package com.twitter.finatra.http.routing

import com.twitter.finatra.conversions.boolean._
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import javax.activation.MimetypesFileTypeMap
import javax.inject.{Inject, Singleton}
import org.apache.commons.io.FilenameUtils

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
  @Flag("doc.root") docRoot: String)
  extends Logging {

  // assertions -- cannot have both doc roots set
  if (localDocRoot.nonEmpty && docRoot.nonEmpty) {
    throw new java.lang.AssertionError(
      "assertion failed: Cannot set both -local.doc.root and -doc.root flags.")
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

  def getContentType(file: String) = {
    extMap.getContentType(
      dottedFileExtension(file))
  }

  /* Private */

  private def isDirectory(path: String): Boolean = {
    path.endsWith("/")
  }

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
    val file = if (new File(path).exists)
      new File(path)
    else
      new File(localDocRoot, path)

    if (file.exists)
      Option(
        new BufferedInputStream(
          new FileInputStream(file)))
    else
      None
  }

  private def dottedFileExtension(uri: String) = {
    '.' + FilenameUtils.getExtension(uri)
  }
}
