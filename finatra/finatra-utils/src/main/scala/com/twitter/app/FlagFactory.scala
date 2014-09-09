package com.twitter.app

object FlagFactory {
  def create[T: Flaggable](name: String, default: => T, help: String): Flag[T] = {
    new Flag[T](name, help, default)
  }
}
