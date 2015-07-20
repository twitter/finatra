package com.twitter.finatra.http.contexts

import com.twitter.finagle.http.Request

object RouteInfo {
  private[http] val field = Request.Schema.newField[Option[RouteInfo]](None)
  private[http] val SanitizeRegexp = "[^A-Za-z0-9_]".r
  private[http] val SlashRegexp = "/".r

  private[http] def set(request: Request, info: RouteInfo): Unit = {
    request.ctx.updateAndLock(field, Some(info))
  }

  def apply(request: Request): Option[RouteInfo] = {
    request.ctx(field)
  }
}

case class RouteInfo(
  name: String,
  path: String) {

  val sanitizedPath = {
    val noSlashes = RouteInfo.SlashRegexp.replaceAllIn(
      target = path.stripPrefix("/").stripSuffix("/")
    , replacement = "_")
    RouteInfo.SanitizeRegexp.replaceAllIn(noSlashes, "")
  }
}
