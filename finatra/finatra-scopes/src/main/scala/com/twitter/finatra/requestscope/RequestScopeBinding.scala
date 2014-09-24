package com.twitter.finatra.requestscope

import com.twitter.finatra.guice.GuiceModule

trait RequestScopeBinding extends GuiceModule {

  override final val modules = Seq(FinagleRequestScopeModule)

  protected def bindRequestScope[T: Manifest] {
    bind[T]
      .toProvider[UnseededFinagleScopeProvider[T]]
      .in[FinagleRequestScoped]
  }
}
