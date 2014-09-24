package com.twitter.finatra.request

import com.twitter.finatra.utils.Logging

object RouteParams {
  val empty = RouteParams()

  def create(pathParams: Map[String, String]) = {
    if (pathParams.isEmpty)
      empty
    else
      RouteParams(pathParams)
  }
}

case class RouteParams(
  map: Map[String, String] = Map())
  extends Logging {

  def apply(name: String): String =
    map(name)

  def get(name: String): Option[String] =
    map.get(name)

  def getOrElse(name: String, default: String): String =
    map.getOrElse(name, default)

  def getInt(name: String): Option[Int] =
    get(name) map {_.toInt}

  def long(name: String): Long =
    getLong(name).get

  def getLong(name: String): Option[Long] =
    get(name) map {_.toLong}

  def getBoolean(name: String): Option[Boolean] =
    get(name) map {_.toBoolean}
}
