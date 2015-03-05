package com.twitter.finatra.test

@deprecated("Use com.twitter.inject.server.FeatureTest and com.twitter.finatra.test.HttpTest")
trait HttpFeatureTest
  extends com.twitter.inject.server.FeatureTest
  with com.twitter.finatra.test.HttpTest