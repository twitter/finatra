package com.posterous.finatra

import org.junit.Test
import com.codahale.simplespec.Spec
import com.twitter.finagle.http.{Http, RichHttp, Request, Response}
import com.twitter.finagle.http.Status._
import org.jboss.netty.handler.codec.http.{DefaultHttpRequest, DefaultHttpResponse, HttpMessage,
    HttpMethod, HttpRequest, HttpVersion, QueryStringEncoder}

class FinatraSpec extends Spec {

  var lastResponse:Response = null

  def get(uri: String, params: Tuple2[String, String]*) = {
    val r = req(HttpMethod.GET, uri, params:_*)
    lastResponse = Router.dispatch(r)
  }

  def post(uri: String, params: Tuple2[String, String]*) = {
    val r = req(HttpMethod.POST, uri, params:_*)
    lastResponse = Router.dispatch(r)
  }
  
  def put(uri: String, params: Tuple2[String, String]*) = {
    val r = req(HttpMethod.PUT, uri, params:_*)
    lastResponse = Router.dispatch(r)
  }

  def delete(uri: String, params: Tuple2[String, String]*) = {
    val r = req(HttpMethod.PUT, uri, params:_*)
    lastResponse = Router.dispatch(r)
  }

  def post(uri: String) = {
    val r       = Request(HttpMethod.POST, uri)
    lastResponse  = Router.dispatch(r)
  }

  def get(uri: String) = {
    val r       = Request(HttpMethod.GET, uri)
    lastResponse  = Router.dispatch(r)
  }
  
  def req(method: HttpMethod, uri: String, params: Tuple2[String, String]*): Request = {
    val encoder = new QueryStringEncoder("")
    var request:Request = null

    params.foreach { case (key, value) =>
      encoder.addParam(key, value)
    }

    if (method == HttpMethod.GET) {
      request = Request(method, encoder.toString)
    }else{
      request = Request(method, uri)
      request.contentType = "application/x-www-form-urlencoded"
      request.contentString = encoder.toString.replaceFirst("\\?", "")
    }
    request
  }

}
