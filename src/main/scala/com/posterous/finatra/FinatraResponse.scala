package com.posterous.finatra

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future

import com.codahale.jerkson.Json._
import com.capotej.finatra_core._

class FinatraApp extends FinatraController {

  def response(body: String, status: Int = 200, headers: Map[String, String] = Map()) = {
    FinatraResponse(status, body, headers)
  }

  def render(status:Int = 200, path: String, layout: String = "application.mustache", exports: Any = "") = {
    new FinatraResponse().template(path).layout(layout).status(status).exports(exports).header("Content-Type", "text/html").build
  }

  def toJson(obj: Any) = {
    new FinatraResponse().json(obj).header("Content-Type", "application/json").build
  }

  def redirect(location: String) = FinatraResponse(301, "moved", Map("Location" -> location))
}

object FinatraResponse {
  def apply(body: String) = new FinatraResponse().body(body).status(200).build
  def apply(status: Int, body: String) = new FinatraResponse().body(body).status(status).build
  def apply(status: Int, body: String, headers: Map[String, String]) = new FinatraResponse().body(body).status(status).headers(headers).build
}

class FinatraResponse {
  var status:Int = 200

  var headers: Map[String, String] = Map()
  var simpleCookies: Map[String, String] = Map()
  var advCookies: Map[String, FinatraCookie] = Map()

  var template: Option[String] = None
  var exports: Option[Any] = None
  var layout: Option[String] = None
  var strBody: Option[String] = None
  var binBody: Option[Array[Byte]] = None
  var json: Option[Any] = None


  def setContent(resp: HttpResponse) = {
    json match {
      case Some(j) =>
        resp.setHeader("Content-Type", "application/json")
        resp.setContent(copiedBuffer(generate(j), UTF_8))
      case None =>
        template match {
          case Some(t) =>
            val l = layout.getOrElse("application.mustache")
            val e = exports.getOrElse("")
            val out = FinatraServer.templateEngine.captureTemplate(t, l, e)
            resp.setContent(copiedBuffer(out, UTF_8))
          case None =>
            strBody match {
              case Some(sb) =>
                resp.setContent(copiedBuffer(sb, UTF_8))
              case None =>
                binBody match {
                  case Some(bb) =>
                    resp.setContent(copiedBuffer(bb))
                  case None =>
                    throw new RuntimeException("nothing to render")
                }
            }
        }
      }
    resp
  }

  def cookie(k: String, v: String) = {
    this.simpleCookies += (k -> v)
    this
  }

  def cookie(c: FinatraCookie) = {
    this.advCookies += (c.value -> c)
    this
  }

  def body(s: String) = {
    this.strBody = Some(s)
    this
  }

  def status(i: Int): FinatraResponse = {
    this.status = i
    this
  }

  def body(b: Array[Byte]) = {
    this.binBody = Some(b)
    this
  }

  def header(k: String, v: String) = {
    this.headers += (k -> v)
    this
  }

  def headers(m: Map[String, String]): FinatraResponse = {
    this.headers = this.headers ++ m
    this
  }

  def json(o: Any): FinatraResponse = {
    this.json = Some(o)
    this
  }

  def template(s: String): FinatraResponse = {
    this.template = Some(s)
    this
  }

  def layout(s: String): FinatraResponse = {
    this.layout = Some(s)
    this
  }

  def exports(o: Any): FinatraResponse = {
    this.exports = Some(o)
    this
  }

  def build = {
    val responseStatus = HttpResponseStatus.valueOf(status)
    val resp = new DefaultHttpResponse(HTTP_1_1, responseStatus)
    var finatraCookies = Map[String, FinatraCookie]()

    headers.foreach { xs =>
      resp.setHeader(xs._1, xs._2)
    }

    simpleCookies.foreach { xs =>
      finatraCookies += (xs._1 -> FinatraCookie(xs._1, xs._2))
    }

    finatraCookies = finatraCookies ++ advCookies

    if (!finatraCookies.isEmpty) {
      resp.setHeader("Cookie", FinatraCookieAdapter.out(finatraCookies))
    }

    Future.value(setContent(resp))
  }

}