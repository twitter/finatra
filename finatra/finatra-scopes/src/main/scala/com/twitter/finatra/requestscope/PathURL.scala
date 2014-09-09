package com.twitter.finatra.requestscope

import com.twitter.finagle.http.Request
import com.twitter.finatra.conversions.httpoption._
import com.twitter.util.Try

object PathURL {

  def create(request: Request): Try[PathURL] = {
    for {
      host <- request.host.toTryOrServerError("Host header not set")
      scheme = request.headerMap.getOrElse("x-forwarded-proto", default = "http")
    } yield {
      PathURL(
        scheme + "://" + addTrailingSlash(request.path))
    }
  }

  private def addTrailingSlash(str: String): String = {
    if (str.endsWith("/"))
      str
    else
      str + "/"
  }
}

/**
 * Full URL with trailing slash (e.g. https://api.server.com/users/)
 */
case class PathURL(
  url: String) {

  override def toString = url //TODO
}