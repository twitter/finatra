package com.twitter.finatra.routing

import com.twitter.finatra.conversions.booleans._
import com.twitter.finatra.utils.Logging
import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import javax.activation.MimetypesFileTypeMap
import javax.inject.Singleton
import org.apache.commons.io.FilenameUtils

// Modified from Finatra 1.x
// In the future, this class should handle caching headers and perform some level of in-memory caching
@Singleton
class FileResolver extends Logging {

  private val localDocRoot = "src%1$smain%1$swebapp%1$s".format(System.getProperty("file.separator"))
  private val extMap = new MimetypesFileTypeMap()
  private val localFileMode = {
    (System.getProperty("env") == "dev").onTrue {
      info("Local file mode enabled")
    }
  }

  /* Public */

  def getInputStream(path: String): Option[InputStream] = {
    assert(path.startsWith("/")) //TODO
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
    for {
      is <- Option(getClass.getResourceAsStream(path))
      bis = new BufferedInputStream(is)
      if bis.available > 0
    } yield bis
  }

  private def getLocalFileInputStream(path: String): Option[FileInputStream] = {
    val file = new File(localDocRoot, path)
    if (validLocalFile(file))
      Option(
        new FileInputStream(file))
    else
      None
  }

  //TODO: Improve
  private def validLocalFile(file: File): Boolean = {
    file.isFile &&
      file.canRead &&
      file.getCanonicalPath.contains(localDocRoot)
  }

  private def dottedFileExtension(uri: String) = {
    '.' + FilenameUtils.getExtension(uri)
  }
}