package com.twitter.finatra

case class Cookie(
  var name: String,
  var value: String,
  var expires: Int = -1,
  var comment: String = null,
  var commentUrl: String = null,
  var domain: String = null,
  var ports: Set[Int] = Set(),
  var path: String = null,
  var version: Int = 0,
  var isDiscard: Boolean = false,
  var isHttpOnly: Boolean = false,
  var isSecure: Boolean = false
)
