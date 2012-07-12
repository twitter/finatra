package com.twitter.finatra

import com.twitter.finatra_core.AbstractFinatraController
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http._


class Controller extends AbstractFinatraController[Request, Response, Future[HttpResponse]] {

  override val responseConverter = new FinatraResponseConverter

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
