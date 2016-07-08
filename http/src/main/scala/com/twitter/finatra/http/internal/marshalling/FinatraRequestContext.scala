package com.twitter.finatra.http.internal.marshalling

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.fileupload.FileUploadException
import com.twitter.io.BufInputStream
import java.io.InputStream
import org.apache.commons.fileupload.RequestContext

private[http] class FinatraRequestContext(request: Request) extends RequestContext {

  override def getCharacterEncoding: String = {
    request.charset.orNull
  }

  override def getContentLength: Int = {
    val contentLengthLong = request.contentLength getOrElse(
      throw new FileUploadException("Content length must be provided."))
    contentLengthLong.toInt
  }

  override def getContentType: String = {
    request.contentType getOrElse(
      throw new FileUploadException("Content type must be provided."))
  }

  override def getInputStream: InputStream = {
    new BufInputStream(request.content)
  }
}
