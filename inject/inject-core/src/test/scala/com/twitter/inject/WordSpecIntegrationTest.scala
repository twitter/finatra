package com.twitter.inject

@deprecated("It is recommended that users switch to com.twitter.inject.IntegrationTest which uses FunSuite", "2017-01-16")
trait WordSpecIntegrationTest
  extends WordSpecTest
  with IntegrationTestMixin
