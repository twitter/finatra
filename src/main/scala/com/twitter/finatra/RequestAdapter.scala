package com.twitter.finatra

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.{Cookie => JCookie}
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer

/**
* Adapts an HttpRequest to a (Finatra) Request
*/

object RequestAdapter {

  def pathOf(x:String) = x.split('?').first

  def paramsOf(request: HttpRequest) = {
    val fakeQs = "/?" + new String(request.getContent.array)
    val qs = new QueryStringDecoder(request.getUri)
    val bs = new QueryStringDecoder(fakeQs)
    var paramsHash = Map[String,String]()
    val allParams = qs.getParameters ++ bs.getParameters

    allParams.foreach { xs =>
      paramsHash += Tuple2(xs._1, xs._2.first)
    }

    paramsHash
  }

  def headersOf(request: HttpRequest) = {
    var headers = Map[String,String]()
    request.getHeaderNames.foreach { name =>
      headers += Tuple2(name, request.getHeader(name))
    }
    headers
  }

  def cookiesOf(request: HttpRequest) = {
    var cookies = Map[String, Cookie]()
    // val cookie = request.getHeader("Cookie")
    // if(cookie != null){
    //   val decodedCookie = new CookieDecoder().decode(cookie)
    //   decodedCookie.foreach { xs =>
    //     cookies += Tuple2(xs.getName, CookieAdapter.in(xs))
    //   }
    // }
    cookies
  }

  def apply(rawRequest: HttpRequest): Request = {
    val request = new Request
    request.path    (pathOf(rawRequest.getUri))
    request.method  (rawRequest.getMethod.toString)
    request.params  (paramsOf(rawRequest))
    request.headers (headersOf(rawRequest))
    //request.cookies (cookiesOf(rawRequest))
    request.body    (rawRequest.getContent.array)
  }
}
