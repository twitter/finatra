/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.finatra

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.twitter.finagle.http.{Response => FinagleResponse, Cookie, Status}
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import org.apache.commons.io.IOUtils
import java.io.File
import org.jboss.netty.handler.codec.http.DefaultCookie
import com.twitter.finatra.serialization.{DefaultJacksonJsonSerializer, JsonSerializer}
import org.jboss.netty.handler.codec.http.{Cookie => NettyCookie, HttpResponseStatus}

object ResponseBuilder {
  def apply(body: String): FinagleResponse =
    new ResponseBuilder().body(body).status(200).build

  def apply(status: Int, body: String): FinagleResponse =
    new ResponseBuilder().body(body).status(status).build

  def apply(status: Int, body: String, headers: Map[String, String]): FinagleResponse =
    new ResponseBuilder().body(body).status(status).headers(headers).build
}

class ResponseBuilder(serializer:JsonSerializer = DefaultJacksonJsonSerializer) extends CommonStatuses {
  private var status:     Option[Int]          = None
  private var headers:    Map[String, String]  = Map()
  private var strBody:    Option[String]       = None
  private var binBody:    Option[Array[Byte]]  = None
  private var json:       Option[Any]          = None
  private var view:       Option[View]         = None
  private var buffer:     Option[ChannelBuffer]= None
  private var cookies:    List[Cookie]         = List()
  private var jsonSerializer: JsonSerializer     = serializer



  def contentType: Option[String] =
    this.headers.get("Content-Type")

  def withSerializer(serializer: JsonSerializer) = {
    jsonSerializer = serializer
    this
  }

  private def setContent(resp: HttpResponse): HttpResponse = {
    json match {
      case Some(j) =>
        resp.headers.set("Content-Type", "application/json")
        val jsonBytes = jsonSerializer.serialize(j)
        resp.headers.set("Content-Length", jsonBytes.length)
        resp.setContent(copiedBuffer(jsonBytes))
      case None =>
        view match {
           case Some(v) =>
             val out = v.render
             val bytes = out.getBytes(UTF_8)
             resp.headers.set("Content-Length", bytes.length)
             if (v.contentType.isDefined && !resp.headers.contains("Content-Type")) {
               resp.headers.set("Content-Type", v.contentType.get)
             }
             resp.setContent(copiedBuffer(bytes))
           case None =>
            strBody match {
              case Some(sb) =>
                val bytes = sb.getBytes(UTF_8)
                resp.headers.set("Content-Length", bytes.length)
                resp.setContent(copiedBuffer(bytes))
              case None =>
                binBody match {
                  case Some(bb) =>
                    resp.headers.set("Content-Length", bb.length)
                    resp.setContent(copiedBuffer(bb))
                  case None =>
                    buffer match {
                      case Some(b) =>
                        resp.headers.set("Content-Length", b.capacity())
                        resp.setContent(b)
                      case None => resp.headers.set("Content-Length", 0) //no content
                    }
                }
            }
        }
      }
    resp
  }

  def cookie(k: String, v: String): ResponseBuilder = {
    this.cookies ::= new Cookie(new DefaultCookie(k, v))
    this
  }

  def cookie(c: Cookie): ResponseBuilder = {
    this.cookies ::= c
    this
  }

  def cookie(c: NettyCookie): ResponseBuilder = {
    this.cookies ::= new Cookie(c)
    this
  }

  def body(s: String): ResponseBuilder = {
    this.strBody = Some(s)
    this
  }

  def status(i: Int): ResponseBuilder = {
    this.status = Some(i)
    this
  }

  def nothing: ResponseBuilder = {
    this.header("Content-Type", "text/plain")
    this.body("")
    this
  }

  def plain(body:String): ResponseBuilder = {
    this.header("Content-Type", "text/plain")
    this.body(body)
    this
  }

  def html(body:String): ResponseBuilder = {
    this.header("Content-Type", "text/html")
    this.body(body)
    this
  }

  def body(b: Array[Byte]): ResponseBuilder = {
    this.binBody = Some(b)
    this
  }

  def header(k: String, v: String): ResponseBuilder = {
    this.headers += (k -> v)
    this
  }

  def headers(m: Map[String, String]): ResponseBuilder = {
    this.headers = this.headers ++ m
    this
  }

  def json(o: Any): ResponseBuilder = {
    this.header("Content-Type", "application/json")
    this.json = Some(o)
    this
  }

  def view(v: View): ResponseBuilder = {
    this.view = Some(v)
    this
  }

  def buffer(b: ChannelBuffer): ResponseBuilder = {
    this.buffer = Some(b)
    this
  }

  def contentType(ct: String): ResponseBuilder = {
    this.header("Content-Type", ct)
    this
  }

  def static(path: String): ResponseBuilder = {
    val fullAssetPath = new File(config.assetPath(), path).toString
    if (FileResolver.hasFile(fullAssetPath) && path != '/') {
      val stream  = FileResolver.getInputStream(fullAssetPath)
      val bytes   = IOUtils.toByteArray(stream)

      stream.read(bytes)

      val mtype = FileService.extMap.getContentType('.' + fullAssetPath.split('.').last)

      this.status(200)
      this.header("Content-Type", mtype)
      this.body(bytes)
    } else {
      throw new IllegalArgumentException("File does not exist")
    }

    this
  }

  def build: FinagleResponse  = {
    build(Request())
  }

  def build(request: Request): FinagleResponse = {
    val response = request.response

    // Only set the status code if set explicitly in the builder
    this.status map response.setStatusCode

    headers.foreach { xs =>
      response.headers.add(xs._1, xs._2)
    }

    cookies foreach response.cookies.add

    setContent(response)

    response
  }

  def toFuture:Future[ResponseBuilder] = Future.value(this)

  override def toString: String = {
    val buf = new StringBuilder

    buf.append(getClass.getSimpleName)
    buf.append('\n')
    buf.append(HTTP_1_1.toString)
    buf.append(' ')
    buf.append(this.status)
    buf.append('\n')
    buf.append(this.headers)

    buf.toString()
  }

}

trait CommonStatuses { self: ResponseBuilder =>
  def ok:                  ResponseBuilder = buildFromStatus(Status.Ok)
  def created:             ResponseBuilder = buildFromStatus(Status.Created)
  def accepted:            ResponseBuilder = buildFromStatus(Status.Accepted)
  def movedPermanently:    ResponseBuilder = buildFromStatus(Status.MovedPermanently)
  def found:               ResponseBuilder = buildFromStatus(Status.Found)
  def notModified:         ResponseBuilder = buildFromStatus(Status.NotModified)
  def temporaryRedirect:   ResponseBuilder = buildFromStatus(Status.TemporaryRedirect)
  def badRequest:          ResponseBuilder = buildFromStatus(Status.BadRequest)
  def unauthorized:        ResponseBuilder = buildFromStatus(Status.Unauthorized)
  def forbidden:           ResponseBuilder = buildFromStatus(Status.Forbidden)
  def notFound:            ResponseBuilder = buildFromStatus(Status.NotFound)
  def gone:                ResponseBuilder = buildFromStatus(Status.Gone)
  def internalServerError: ResponseBuilder = buildFromStatus(Status.InternalServerError)
  def notImplemented:      ResponseBuilder = buildFromStatus(Status.NotImplemented)
  def serviceUnavailable:  ResponseBuilder = buildFromStatus(Status.ServiceUnavailable)

  private def buildFromStatus(st: HttpResponseStatus): ResponseBuilder  = {
    status(st.getCode)
    this
  }

}
