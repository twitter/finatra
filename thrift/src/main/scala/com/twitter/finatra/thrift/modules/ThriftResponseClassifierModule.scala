package com.twitter.finatra.thrift.modules

import com.twitter.finatra.thrift.response.ThriftResponseClassifier
import com.twitter.inject.TwitterModule

object ThriftResponseClassifierModule extends TwitterModule {

  override def configure(): Unit = {
    bindSingleton[ThriftResponseClassifier].toInstance(
      ThriftResponseClassifier.ThriftExceptionsAsFailures
    )
  }
}
