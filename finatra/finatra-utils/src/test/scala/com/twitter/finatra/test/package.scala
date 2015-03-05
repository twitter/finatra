package com.twitter.finatra

package object test {

  @deprecated("com.twitter.inject.server.FeatureTest")
  type TwitterServerFeatureTest = com.twitter.inject.server.FeatureTest

  @deprecated("com.twitter.inject.Test")
  type Test = com.twitter.inject.Test

  @deprecated("Use com.twitter.inject.Mockito")
  type Mockito = com.twitter.inject.Mockito
}
