package com.twitter.finatra

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.{Cookie => JCookie}
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future

import com.codahale.jerkson.Json._
import com.twitter.finatra_core._

object Response {
  def apply(body: String) = new Response().body(body).status(200).build
  def apply(status: Int, body: String) = new Response().body(body).status(status).build
  def apply(status: Int, body: String, headers: Map[String, String]) = new Response().body(body).status(status).headers(headers).build
}

class Response {
  var status:Int = 200

  var headers: Map[String, String] = Map()
  var simpleCookies: Map[String, String] = Map()
  //var advCookies: Map[String, FinatraCookie] = Map()

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
        //template match {
          // case Some(t) =>
          //   val l = layout.getOrElse("application.mustache")
          //   val e = exports.getOrElse("")
          //   val out = FinatraServer.templateEngine.captureTemplate(t, l, e)
          //   resp.setContent(copiedBuffer(out, UTF_8))
          // case None =>
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
        //}
      }
    resp
  }

  def cookie(k: String, v: String) = {
    this.simpleCookies += (k -> v)
    this
  }

  // def cookie(c: Cookie) = {
  //   this.advCookies += (c.value -> c)
  //   this
  // }

  def body(s: String) = {
    this.strBody = Some(s)
    this
  }

  def status(i: Int): Response = {
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

  def headers(m: Map[String, String]): Response = {
    this.headers = this.headers ++ m
    this
  }

  def json(o: Any): Response = {
    this.json = Some(o)
    this
  }

  def template(s: String): Response = {
    this.template = Some(s)
    this
  }

  def layout(s: String): Response = {
    this.layout = Some(s)
    this
  }

  def exports(o: Any): Response = {
    this.exports = Some(o)
    this
  }

  def build = {
    val responseStatus = HttpResponseStatus.valueOf(status)
    val resp = new DefaultHttpResponse(HTTP_1_1, responseStatus)
    //var cookies = Map[String, Cookie]()

    headers.foreach { xs =>
      resp.setHeader(xs._1, xs._2)
    }

    // simpleCookies.foreach { xs =>
    //   cookies += (xs._1 -> Cookie(xs._1, xs._2))
    // }

    // cookies = cookies ++ advCookies

    // if (!cookies.isEmpty) {
    //   resp.setHeader("Cookie", CookieAdapter.out(cookies))
    // }

    Future.value(setContent(resp))
  }

}
