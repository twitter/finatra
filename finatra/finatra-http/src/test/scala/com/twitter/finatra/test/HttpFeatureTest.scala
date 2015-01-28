package com.twitter.finatra.test

@deprecated("use TwitterServerFeatureTest", "1/22/15")
trait HttpFeatureTest
  extends TwitterServerFeatureTest
  with HttpTest