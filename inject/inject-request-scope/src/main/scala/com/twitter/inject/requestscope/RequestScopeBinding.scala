package com.twitter.inject.requestscope

import com.twitter.inject.TwitterModule

trait RequestScopeBinding extends TwitterModule {

  override final val modules = Seq(FinagleRequestScopeModule)

  protected def bindRequestScope[T: Manifest](): Unit = {
    bind[T]
      .toProvider[UnseededFinagleScopeProvider[T]]
      .in[FinagleRequestScoped]
  }
}
