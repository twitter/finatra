package com.twitter.finatra.fileupload

import com.twitter.finagle.http.Request
import java.io.InputStream
import org.apache.commons.fileupload.RequestContext
import org.jboss.netty.buffer.ChannelBufferInputStream

class FinatraRequestContext(request: Request) extends RequestContext {

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
    new ChannelBufferInputStream(request.content)
  }
}
