package com.twitter.finatra.http.routing

abstract class Scope {
  def prefix: String
  def scoped(path: String) = prefix + path
}

object Scope {
  val empty = new Scope {def prefix = ""}
}
