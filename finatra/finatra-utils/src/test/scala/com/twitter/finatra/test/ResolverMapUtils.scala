package com.twitter.finatra.test

object ResolverMapUtils {

  def resolverMapStr(resolverMap: Map[String, String]): Array[String] = {
    if (resolverMap.isEmpty)
      Array()
    else
      Array(
        "-com.twitter.server.resolverMap=" + {
          resolverMap map { case (k, v) =>
            k + "=" + v
          } mkString ","
        })
  }
}