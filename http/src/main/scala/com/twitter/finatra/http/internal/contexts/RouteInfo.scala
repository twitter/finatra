package com.twitter.finatra.http.internal.contexts

import com.twitter.finagle.http.Request
import org.jboss.netty.handler.codec.http.HttpMethod

private[http] object RouteInfo {
  private val field = Request.Schema.newField[Option[RouteInfo]](None)

  private[internal] def set(request: Request, info: RouteInfo): Unit = {
    request.ctx.updateAndLock(field, Some(info))
  }

  def apply(request: Request): Option[RouteInfo] = {
    request.ctx(field)
  }
}

private[http] case class RouteInfo(
  name: String,
  method: HttpMethod)
