package com.twitter.inject

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner

@deprecated("It is recommended that users switch to com.twitter.inject.Test which uses FunSuite", "2017-01-16")
@RunWith(classOf[JUnitRunner])
abstract class WordSpecTest
  extends WordSpec
  with TestMixin
