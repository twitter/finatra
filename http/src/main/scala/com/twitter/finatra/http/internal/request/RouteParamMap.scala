package com.twitter.finatra.http.internal.request

import com.twitter.finagle.http.ParamMap

private[http] class RouteParamMap(
  paramMap: => ParamMap, //avoid constructing paramMap from Finagle request unless needed
  params: Map[String, String])
  extends ParamMap {

  override def isValid = paramMap.isValid

  override def get(name: String): Option[String] = {
    params.get(name) orElse paramMap.get(name)
  }

  override def getAll(name: String): Iterable[String] = {
    params.get(name).toIterable ++ paramMap.getAll(name)
  }

  override def iterator: Iterator[(String, String)] = {
    params.iterator ++ paramMap.iterator
  }
}
