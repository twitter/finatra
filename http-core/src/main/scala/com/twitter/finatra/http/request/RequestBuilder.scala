package com.twitter.finatra.http.request

import com.twitter.finagle.http.{RequestBuilder => FinagleRequestBuilder, _}
import com.twitter.finatra.http.fileupload.MultipartItem
import com.twitter.io.{Buf, StreamIO}
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Provides a class for building <code>finagle.http.Request</code> objects
 */
object RequestBuilder {
  def get(url: String): RequestBuilder = {
    create(Method.Get, url)
  }

  def post(url: String): RequestBuilder = {
    create(Method.Post, url)
  }

  def put(url: String): RequestBuilder = {
    create(Method.Put, url)
  }

  def patch(url: String): RequestBuilder = {
    create(Method.Patch, url)
  }

  def delete(url: String): RequestBuilder = {
    create(Method.Delete, url)
  }

  def head(url: String): RequestBuilder = {
    create(Method.Head, url)
  }

  def trace(url: String): RequestBuilder = {
    create(Method.Trace, url)
  }

  def connect(url: String): RequestBuilder = {
    create(Method.Connect, url)
  }

  def options(url: String): RequestBuilder = {
    create(Method.Options, url)
  }

  def create(method: Method, url: String): RequestBuilder = {
    new RequestBuilder(Request(method, url))
  }
}

/**
 * RequestBuilder is a finagle.http.Request with a builder API for common mutations
 */
class RequestBuilder(override val request: Request) extends RequestProxy {

  def headers(headers: Map[String, String]): RequestBuilder = {
    for {
      (key, value) <- headers
    } {
      request.headerMap.set(key, value)
    }
    this
  }

  def headers(elems: (String, String)*): RequestBuilder = {
    headers(elems.toMap)
  }

  def headers(elems: Iterable[(String, String)]): RequestBuilder = {
    headers(elems.toMap)
  }

  def header(key: String, value: AnyRef): RequestBuilder = {
    request.headerMap.set(key, value.toString)
    this
  }

  def chunked: RequestBuilder = {
    request.setChunked(true)
    this
  }

  def body(string: String, contentType: String = Message.ContentTypeJson): RequestBuilder = {
    request.setContentString(string)
    request.headerMap.set(Fields.ContentLength, string.getBytes(UTF_8).length.toString)
    request.headerMap.set(Fields.ContentType, contentType)
    this
  }

  def bodyFromResource(
    resource: String,
    contentType: String = Message.ContentTypeJson
  ): RequestBuilder = {
    val bodyStream = getClass.getResourceAsStream(resource)
    body(StreamIO.buffer(bodyStream).toString(), contentType)
  }

  /**
   * Build a multipart/form-data POST request.
   * @param items a list of [[MultipartItem]]
   * @return a RequestBuilder to build a multipart/form-data POST request
   *
   * @note When using this api, the request must already be configured as
   *       a Post request, otherwise an [[IllegalArgumentException]] will
   *       be thrown.
   * @note This method isn't additive, when called multiple times, it will
   *       override the content that was previously set on the request.
   */
  def bodyMultipart(items: Seq[MultipartItem]): RequestBuilder = {
    // a multipart request is a Post request
    require(request.method == Method.Post)
    val fileElement: Seq[FileElement] = items.map { item =>
      FileElement(
        name = item.fieldName,
        content = Buf.ByteArray(item.data: _*),
        contentType = item.contentType,
        filename = item.filename
      )
    }

    // leveraging Finagle RequestBuilder to build
    // the multipart content which is used to set on
    // the Request built by Finatra RequestBuilder
    val multipartReq: Request = FinagleRequestBuilder()
    // the url is an artifact to build the request
    // it is ok to use a fake one since we only care
    // about the content of the request
      .url("http://my-fake-request-to-throw-away.com")
      .add(fileElement)
      .buildFormPost(multipart = true)

    assert(!multipartReq.content.isEmpty)
    request.content = multipartReq.content
    // a multipart request's contentType will not be empty
    request.contentType = multipartReq.contentType.get
    this
  }
}
