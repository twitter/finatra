package com.twitter.finatra.http.contexts

import com.twitter.finagle.http.Request

object RouteInfo {
  private[http] val field = Request.Schema.newField[Option[RouteInfo]](None)
  private[http] val SanitizeRegexp = "[^A-Za-z0-9_]".r
  private[http] val SlashRegexp = "/".r

  def apply(request: Request): Option[RouteInfo] = {
    request.ctx(field)
  }

  private[http] def set(request: Request, info: RouteInfo): Unit = {
    request.ctx.update(field, Some(info))
  }

  private[http] def sanitize(path: String): String = {
    RouteInfo.SanitizeRegexp.replaceAllIn(
      RouteInfo.SlashRegexp
        .replaceAllIn(target = path.stripPrefix("/").stripSuffix("/"), replacement = "_"),
      "")
  }
}

case class RouteInfo(name: String, path: String) {
  import RouteInfo._

  val sanitizedPath: String = sanitize(path)
}
