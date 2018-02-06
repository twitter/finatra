package com.twitter.inject.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

object ThriftClientIdModule extends ThriftClientIdModule

/**
 * Provides a [[com.twitter.finagle.thrift.ClientId]] binding with a value
 * configurable via a [[com.twitter.app.Flag]].
 *
 * @see [[https://twitter.github.io/finatra/user-guide/getting-started/flags.html Flags]]
 */
class ThriftClientIdModule extends TwitterModule {
  private val clientIdFlag = flag("thrift.clientId", "", "Thrift client id")

  @Provides
  @Singleton
  def providesClientId: ClientId = ClientId(clientIdFlag())
}
