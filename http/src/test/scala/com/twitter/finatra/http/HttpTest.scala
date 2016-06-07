package com.twitter.finatra.http

import java.net.URLEncoder

trait HttpTest
  extends com.twitter.inject.Test {

  def resolverMap(resolverMap: (String, String)*): String = {
    if (resolverMap.isEmpty)
      ""
    else
      "-com.twitter.server.resolverMap=" + {
        resolverMap map { case (k, v) =>
          k + "=" + v
        } mkString ","
      }
  }

  def resolverMap(name: String, httpServer: EmbeddedHttpServer): (String, String) = {
    ("com.twitter.server.resolverMap", name + "=" + httpServer.externalHttpHostAndPort)
  }

  def urlEncode(str: String) = {
    URLEncoder.encode(str, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }
}
