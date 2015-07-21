package com.twitter.finatra

package object guice {

  @deprecated("Use com.twitter.inject.TwitterModule", "")
  type GuiceModule = com.twitter.inject.TwitterModule

  @deprecated("Use com.twitter.inject.app.App", "")
  type GuiceApp = com.twitter.inject.app.App

  @deprecated("Use com.twitter.inject.Injector", "")
  type FinatraInjector = com.twitter.inject.Injector
}
