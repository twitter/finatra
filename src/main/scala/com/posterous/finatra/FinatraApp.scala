package com.posterous.finatra

import com.twitter.finatra_core.AbstractFinatraController
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http._


class FinatraApp extends AbstractFinatraController[Request, Future[HttpResponse]] {

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
