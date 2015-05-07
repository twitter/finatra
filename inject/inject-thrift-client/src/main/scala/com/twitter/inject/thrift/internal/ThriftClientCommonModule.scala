package com.twitter.inject.thrift.internal

import com.google.inject.Provides
import com.twitter.finagle.Filter
import com.twitter.finagle.thrift.Protocols
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import org.apache.thrift.protocol.TProtocolFactory

object ThriftClientCommonModule extends TwitterModule {

  @Provides
  @Singleton
  def providesProtocolFactory: TProtocolFactory = {
    Protocols.binaryFactory()
  }

  @Provides
  @Singleton
  def providesTerminatingThriftClientFilter: TerminatingThriftClientFilter = {
    TerminatingThriftClientFilter(Filter.identity)
  }
}
