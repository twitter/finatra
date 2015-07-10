package com.twitter.finatra.http.contexts

import com.twitter.finagle.http.Request
import org.jboss.netty.handler.codec.http.HttpMethod

object RouteInfo {
  private[http] val field = Request.Schema.newField[Option[RouteInfo]](None)

  private[http] def set(request: Request, info: RouteInfo): Unit = {
    request.ctx.updateAndLock(field, Some(info))
  }

  def apply(request: Request): Option[RouteInfo] = {
    request.ctx(field)
  }
}

case class RouteInfo(
  name: String,
  method: HttpMethod)
