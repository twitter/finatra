package com.twitter.inject.app.tests.internal

import javax.inject.Singleton

@Singleton
class StateMap {
  val internals = scala.collection.mutable.HashMap[String, Int]()
}
