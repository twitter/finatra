package com.twitter.finatra.http.routing

class MergedPrefix(
  prefixes: Prefix*
) extends Prefix {

  private val CombinedPrefix = prefixes reduceLeft {_ andThen _}

  def prefix: String = CombinedPrefix.prefix
}
