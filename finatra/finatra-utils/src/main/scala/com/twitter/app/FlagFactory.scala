package com.twitter.app

object FlagFactory {
  def create[T: Flaggable](name: String, default: => T, help: String): Flag[T] = {
    new Flag[T](name, help, default)
  }

  def create[T: Flaggable : Manifest](name: String, help: String): Flag[T] = {
    new Flag[T](name, help, manifest[T].runtimeClass.toString)
  }
}
