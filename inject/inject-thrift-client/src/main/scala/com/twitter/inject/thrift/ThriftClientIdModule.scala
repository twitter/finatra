package com.twitter.inject.thrift

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.TwitterModule

object ThriftClientIdModule extends TwitterModule {
  private val clientIdFlag = flag("thrift.clientId", "", "Thrift client id")

  @Provides
  @Singleton
  def providesClientId: ClientId = ClientId(clientIdFlag())
}
