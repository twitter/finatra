package com.twitter.finatra

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.{Cookie => JCookie}
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import com.twitter.finatra_views.View

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

  var template: Option[String]      = None
  var exports: Option[Any]          = None
  var layout: Option[String]        = None
  var strBody: Option[String]       = None
  var binBody: Option[Array[Byte]]  = None
  var json: Option[Any]             = None
  var view: Option[View]            = None

  def setContent(resp: HttpResponse) = {
    json match {
      case Some(j) =>
        resp.setHeader("Content-Type", "application/json")
        resp.setContent(copiedBuffer(generate(j), UTF_8))
      case None =>
        view match {
           case Some(v) =>
             val out = v.render
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

  def ok = {
    status(200)
    this
  }

  def notFound = {
    status(404)
    this
  }

  def body(s: String) = {
    this.strBody = Some(s)
    this
  }

  def status(i: Int): Response = {
    this.status = i
    this
  }

  def plain(body:String) = {
    this.header("Content-Type", "text/plain")
    this.body(body)
    this
  }

  def html(body:String) = {
    this.header("Content-Type", "text/html")
    this.body(body)
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
    this.header("Content-Type", "application/json")
    this.json = Some(o)
    this
  }

  def view(v: View): Response = {
    this.view = Some(v)
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
