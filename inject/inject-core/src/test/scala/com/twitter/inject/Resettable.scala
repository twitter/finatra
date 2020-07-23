package com.twitter.inject

@deprecated("No replacement.", "2020-07-22")
trait Resettable {
  def reset(): Unit
}
