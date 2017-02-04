package com.twitter.inject

import org.scalatest.WordSpec

@deprecated("It is recommended that users switch to com.twitter.inject.Test which uses FunSuite", "2017-01-16")
abstract class WordSpecTest
  extends WordSpec
  with TestMixin
