package com.twitter.inject.server

import com.twitter.inject.WordSpecTest

@deprecated(
  "It is recommended that users switch to com.twitter.inject.server.FeatureTest which uses FunSuite",
  "2017-01-16"
)
trait WordSpecFeatureTest extends WordSpecTest with FeatureTestMixin
