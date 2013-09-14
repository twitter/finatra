package com.twitter.finatra.test

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

abstract class FlatSpecHelper extends FlatSpec with ShouldMatchers with SpecHelper
