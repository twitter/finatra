package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.http.Response
import com.twitter.finatra.utils.Logging
import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import javax.activation.MimetypesFileTypeMap
import javax.inject.Singleton
import org.apache.commons.io.IOUtils
import org.jboss.netty.buffer.ChannelBuffers

// Modified from Finatra 1.x
// In the future, this class should handle caching headers and perform some level of in-memory caching
@Singleton
class FileResolver extends Logging {

  private val localDocRoot = "src%1$smain%1$swebapp%1$s".format(System.getProperty("file.separator"))
  private val extMap = new MimetypesFileTypeMap()
  private val localFileMode = {
    infoResult("Local file mode: %s") {
      System.getProperty("env") == "dev"
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
    extMap.getContentType(file)
  }

  def getResponse(requestUri: String, response: Response): Option[Response] = {
    for (is <- getInputStream(requestUri)) yield {
      response.headers.set(
        "Content-Type",
        extMap.getContentType(
          fileExtension(requestUri)))

      response.setContent(
        createChannelBuffer(is))

      response
    }
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

  private def fileExtension(uri: String) = {
    '.' + uri.toString.split('.').last
  }

  //TODO: Optimize
  private def createChannelBuffer(inputStream: InputStream) = {
    ChannelBuffers.copiedBuffer(
      IOUtils.toByteArray(inputStream))
  }
}