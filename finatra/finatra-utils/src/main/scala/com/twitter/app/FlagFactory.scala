package com.twitter.app

import scala.reflect.runtime.universe.{TypeTag, typeOf}

object FlagFactory {
  def create[T: Flaggable](name: String, default: => T, help: String): Flag[T] = {
    new Flag[T](name, help, default)
  }

  def create[T: Flaggable : TypeTag](name: String, help: String): Flag[T] = {
    new Flag[T](name, help, typeOf[T].toString)
  }
}
