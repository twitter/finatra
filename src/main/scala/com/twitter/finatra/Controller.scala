package com.twitter.finatra

import com.twitter.finatra_core.{AbstractFinatraController, DispatchHandler}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http._



class ResponseCallbackHandler extends DispatchHandler[Response, Future[HttpResponse]] {
  override def apply(resp: Response) = {
    resp.build
  }
}

class Controller extends AbstractFinatraController[Request, Response, Future[HttpResponse]] {

  override val dispatchHandler = new ResponseCallbackHandler

  def response(body: String, status: Int = 200, headers: Map[String, String] = Map()) = {
    Response(status, body, headers)
  }

  def render = {
    new Response
  }

  def toJson(obj: Any) = {
    new Response().json(obj).header("Content-Type", "application/json").build
  }

  def redirect(location: String) = Response(301, "moved", Map("Location" -> location))
}
