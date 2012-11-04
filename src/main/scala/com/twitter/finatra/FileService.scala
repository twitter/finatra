package com.twitter.finatra

import com.twitter.finagle.{Service, SimpleFilter}
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.twitter.util.Future
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}

import org.apache.commons.io.IOUtils
import java.io.{FileInputStream, File, InputStream}
import org.jboss.netty.handler.codec.http.HttpMethod

object FileResolver {

  def hasFile(path: String):Boolean = {
    if(System.getProperty("env") == "production"){
      hasResourceFile(path)
    } else {
      hasLocalFile(path)
    }
  }

  def getInputStream(path: String): InputStream = {
    if(System.getProperty("env") == "production"){
      getResourceInputStream(path)
    } else {
      getLocalInputStream(path)
    }
  }

  private def getResourceInputStream(path: String): InputStream = {
    getClass.getResourceAsStream(path)
  }

  private def getLocalInputStream(path: String): InputStream = {
    val file = new File("src/main/resources", path)
    new FileInputStream(file)
  }

  private def hasResourceFile(path: String):Boolean = {
    val fi = getClass.getResourceAsStream(path)
    var result = false
    try {
      if (fi != null && fi.available > 0) {
        result = true
      } else {
        result = false
      }
    } catch {
      case e: Exception =>
        result = false
    }
    result
  }

  private def hasLocalFile(path: String):Boolean = {
    val file = new File("src/main/resources", path)
    if(file.toString.contains(".."))     return false
    if(!file.exists || file.isDirectory) return false
    if(!file.canRead)                    return false
    true
  }


}

object FileService {

  //lifted from tiscaf - http://gaydenko.com/scala/tiscaf/httpd/
  val exts = Map(
    "html" -> "text/html",
    "htm" -> "text/html",
    "js" -> "application/x-javascript",
    "css" -> "text/css ",
    "shtml" -> "text/html",
    "gif" -> "image/gif",
    "ico" -> "image/x-icon",
    "jpeg" -> "image/jpeg ",
    "jpg" -> "image/jpeg ",
    "png" -> "image/png",
    "pdf" -> "application/pdf",
    "zip" -> "application/zip",
    "xhtml" -> "application/xhtml+xml",
    "xht" -> "application/xhtml+xml",
    "svg" -> "image/svg+xml",
    "svgz" -> "image/svg+xml",
    "tiff" -> "image/tiff",
    "tif" -> "image/tiff",
    "djvu" -> "image/vnd.djvu",
    "djv" -> "image/vnd.djvu",
    "bmp" -> "image/x-ms-bmp",
    "asc" -> "text/plain",
    "txt" -> "text/plain",
    "text" -> "text/plain",
    "diff" -> "text/plain",
    "scala" -> "text/plain",
    "xml" -> "application/xml",
    "xsl" -> "application/xml",
    "tgz" -> "application/x-gtar",
    "jar" -> "application/java-archive",
    "class" -> "application/java-vm",
    "flac" -> "application/x-flac",
    "ogg" -> "application/ogg",
    "wav" -> "audio/x-wav",
    "pgp" -> "application/pgp-signatur",
    "ps" -> "application/postscript",
    "eps" -> "application/postscript",
    "rar" -> "application/rar",
    "rdf" -> "application/rdf+xml",
    "rss" -> "application/rss+xml",
    "torrent" -> "application/x-bittorrent",
    "deb" -> "application/x-debian-package",
    "udeb" -> "application/x-debian-package",
    "dvi" -> "application/x-dvi",
    "gnumeric" -> "application/x-gnumeric",
    "iso" -> "application/x-iso9660-image",
    "jnlp" -> "application/x-java-jnlp-file",
    "latex" -> "application/x-latex",
    "rpm" -> "application/x-redhat-package-manager",
    "tar" -> "application/x-tar",
    "texinfo" -> "application/x-texinfo",
    "texi" -> "application/x-texinfo",
    "man" -> "application/x-troff-man",
    "h++" -> "text/x-c++hdr",
    "hpp" -> "text/x-c++hdr",
    "hxx" -> "text/x-c++hdr",
    "hh" -> "text/x-c++hdr",
    "c++" -> "text/x-c++src",
    "cpp" -> "text/x-c++src",
    "cxx" -> "text/x-c++src",
    "cc" -> "text/x-c++src",
    "h" -> "text/x-chdr",
    "hs" -> "text/x-haskell",
    "java" -> "text/x-java",
    "lhs" -> "text/x-literate-haskell",
    "pas" -> "text/x-pascal",
    "py" -> "text/x-python",
    "xul" -> "application/vnd.mozilla.xul+xml",
    "odc" -> "application/vnd.oasis.opendocument.chart",
    "odb" -> "application/vnd.oasis.opendocument.database",
    "odf" -> "application/vnd.oasis.opendocument.formula",
    "odg" -> "application/vnd.oasis.opendocument.graphics",
    "odi" -> "application/vnd.oasis.opendocument.image",
    "odp" -> "application/vnd.oasis.opendocument.presentation",
    "ods" -> "application/vnd.oasis.opendocument.spreadsheet",
    "odt" -> "application/vnd.oasis.opendocument.text",
    "abw" -> "application/x-abiword",
    "md" -> "text/x-markdown",
    "markdown" -> "text/x-markdown"
  )

  val gzipable = List(
    "text/html",
    "application/x-javascript",
    "text/css ",
    "text/plain",
    "application/xml",
    "application/xhtml+xml",
    "image/svg+xml",
    "application/rdf+xml",
    "application/rss+xml",
    "text/x-c++hdr",
    "text/x-c++src",
    "text/x-chdr",
    "text/x-haskell",
    "text/x-java",
    "text/x-python"
  )

}

class FileService extends SimpleFilter[FinagleRequest, FinagleResponse] with Logging {
  def isValidPath(path: String):Boolean = {
    val fi = getClass.getResourceAsStream(path)
    var result = false
    try {
      if (fi != null && fi.available > 0) {
        result = true
      } else {
        result = false
      }
    } catch {
      case e: Exception =>
        result = false
    }
    result
  }

  def apply(request: FinagleRequest, service: Service[FinagleRequest, FinagleResponse]) = {
    if (FileResolver.hasFile(request.uri)) {
      val fh = FileResolver.getInputStream(request.uri)
      val b = IOUtils.toByteArray(fh)
      fh.read(b)
      val response = request.response
      val mtype = FileService.exts.get(request.uri.toString.split('.').last).getOrElse("application/octet-stream")
      response.status = OK
      response.setHeader("Content-Type", mtype)
      response.setContent(copiedBuffer(b))
      Future.value(response)
    } else {
      service(request)
    }
  }
}