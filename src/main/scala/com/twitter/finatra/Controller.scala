package com.twitter.finatra

import com.twitter.finatra_core.AbstractFinatraController
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http._


class Controller
  extends AbstractFinatraController[Request, Response, Future[HttpResponse]]
  with Logging {

  override val responseConverter = new FinatraResponseConverter

  def render = new Response

  def redirect(location: String, message: String = "moved") = {
    render.plain(message).status(301).header("Location", location)
  }
}
