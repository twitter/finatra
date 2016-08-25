package com.twitter.finagle.http

import com.twitter.finagle.netty3.BufChannelBuffer
import com.twitter.io.Buf
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.handler.codec.http._

/**
 * The [[org.jboss.netty.handler.codec.http.DefaultHttpMessage]] uses the
 * [[org.jboss.netty.handler.codec.http.DefaultHttpHeaders]] with a default of 'true' for
 * the validate constructor param. This implementation of validate scans any set header value
 * character by character unless the value exactly matches an entry in KNOWN_VALUES.
 * @see [[org.jboss.netty.handler.codec.http.HttpHeaders.Values]]. In micro-benchmarking
 * this has been shown to have a significant negative performance impact. Especially, for
 * "simple" or highly-optimized HTTP endpoints.
 *
 * NonValidatingHttpHeadersResponse constructs a new
 * [[org.jboss.netty.handler.codec.http.HttpResponse]] with an implementation of
 * [[org.jboss.netty.handler.codec.http.DefaultHttpHeaders]] where validate is 'false'
 * for the express purpose of not validating any set Response header values.
 * @param status - [[org.jboss.netty.handler.codec.http.HttpResponseStatus]]
 * @param content - [[com.twitter.io.Buf]] response body
 * @param contentType - String representation of response content-type.
 */
@deprecated("This class is an optimization over Netty3 header handling and not intended for wide usage.", "2016-08-23")
private[twitter] class NonValidatingHttpHeadersResponse(
  status: HttpResponseStatus,
  content: Buf,
  contentType: String)
  extends Response {

  override protected[finagle] val httpResponse: HttpResponse = new HttpResponse {
    private val httpHeaders = new DefaultHttpHeaders(false)
    httpHeaders.add(Fields.ContentType, contentType)

    override def getStatus: HttpResponseStatus = status

    override val getContent: ChannelBuffer = BufChannelBuffer(content)

    override def isChunked: Boolean = false

    override def getProtocolVersion: HttpVersion = HttpVersion.HTTP_1_1

    override def headers(): HttpHeaders = httpHeaders

    override def setContent(content: ChannelBuffer): Unit = ???

    override def setProtocolVersion(version: HttpVersion): Unit = ???

    override def setChunked(chunked: Boolean): Unit = ???

    override def setStatus(status: HttpResponseStatus): Unit = ???
  }
}
