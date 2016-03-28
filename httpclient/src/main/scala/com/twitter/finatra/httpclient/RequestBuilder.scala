package com.twitter.finatra.httpclient

import com.google.common.net.HttpHeaders
import com.twitter.finagle.http.{Message, Method, Request, RequestProxy}
import com.twitter.io.Charsets
import org.apache.commons.io.IOUtils

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
    new RequestBuilder(
      Request(method, url))
  }
}

/**
 * RequestBuilder is a finagle.http.Request with a builder API for common mutations
 */
class RequestBuilder(
  override val request: Request)
  extends RequestProxy {

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

  def chunked = {
    request.setChunked(true)
    this
  }

  def body(string: String, contentType: String = Message.ContentTypeJson): RequestBuilder = {
    request.setContentString(string)
    request.headerMap.set(HttpHeaders.CONTENT_LENGTH, string.getBytes(Charsets.Utf8).length.toString)
    request.headerMap.set(HttpHeaders.CONTENT_TYPE, contentType)
    this
  }

  def bodyFromResource(resource: String, contentType: String = Message.ContentTypeJson): RequestBuilder = {
    val bodyStream = getClass.getResourceAsStream(resource)
    body(IOUtils.toString(bodyStream), contentType)
  }
}
