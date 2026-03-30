package com.twitter.inject.app.tests

import jakarta.inject.Singleton

@Singleton
class StateMap {
  val internals = scala.collection.mutable.HashMap[String, Int]()
}
