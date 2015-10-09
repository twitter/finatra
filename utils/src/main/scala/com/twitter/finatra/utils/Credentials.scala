package com.twitter.finatra.utils

case class Credentials(
  underlying: Map[String, String]) {

  val isEmpty = underlying.isEmpty

  def get(key: String): Option[String] = {
    underlying.get(key)
  }
}