package com.twitter.finatra.http.routing

abstract class Prefix { self =>
  def prefix: String
  def apply(path: String) = prefix + path
  def andThen(other: Prefix): Prefix = new Prefix { def prefix = self.prefix + other.prefix }
}

object Prefix {
  val empty = new Prefix { def prefix = "" }
}
