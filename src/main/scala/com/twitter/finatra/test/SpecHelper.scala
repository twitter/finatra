package com.twitter.finatra.test

import com.twitter.finatra._
import com.twitter.finatra_core.{AbstractFinatraSpec}
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import com.twitter.util.Future
import scala.collection.mutable.Map
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.util.CharsetUtil.UTF_8

class MockResponse(val originalResponse: HttpResponse) {

  def status                  = originalResponse.getStatus
  def code                    = originalResponse.getStatus.getCode
  def body                    = originalResponse.getContent.toString(UTF_8)
  def getHeader(name: String) = originalResponse.getHeader(name)
  def getHeaders              = originalResponse.getHeaders

}

abstract class SpecHelper extends AbstractFinatraSpec[Request, Response, Future[HttpResponse]] {

  def response  = new MockResponse(lastResponse.get)
  def request   = new Request

  var lastResponse:Future[HttpResponse] = null

  def buildRequest(method:String, path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    val req = request
    req.method(method)
    req.path(path)
    req.params(params)
    req.headers(headers)
    lastResponse = app.dispatch(req).asInstanceOf[Option[Future[HttpResponse]]].get
  }
}
