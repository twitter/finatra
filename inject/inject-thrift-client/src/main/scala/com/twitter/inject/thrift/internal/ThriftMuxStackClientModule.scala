package com.twitter.inject.thrift.internal

import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

object ThriftMuxStackClientModule extends TwitterModule {

  @Provides
  @Singleton
  def providesThriftMuxClient: ThriftMux.Client = {
    ThriftMux.Client()
  }
}

