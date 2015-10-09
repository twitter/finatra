package com.twitter.finatra.modules

import com.google.inject.Provides
import com.twitter.finatra.utils.Credentials
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

class InMemoryCredentialsModule(
  credentials: Map[String, String])
  extends TwitterModule {

  @Singleton
  @Provides
  def providesCredentials: Credentials = {
    Credentials(credentials)
  }
}
