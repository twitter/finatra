package com.twitter.inject.server

import com.twitter.inject.Test

trait FeatureTest
  extends Test
  with FeatureTestMixin
