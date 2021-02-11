package com.twitter.finatra.http.fileupload

import com.twitter.finagle.http.Request
import com.twitter.io.BufInputStream
import java.io.InputStream

private[fileupload] class RequestContext(request: Request)
    extends org.apache.commons.fileupload.RequestContext {

  override def getCharacterEncoding: String = {
    request.charset.orNull
  }

  override def getContentLength: Int = {
    val contentLengthLong = request.contentLength getOrElse (throw new FileUploadException(
      "Content length must be provided."
    ))
    contentLengthLong.toInt
  }

  override def getContentType: String = {
    request.contentType getOrElse (throw new FileUploadException("Content type must be provided."))
  }

  override def getInputStream: InputStream = {
    new BufInputStream(request.content)
  }
}
