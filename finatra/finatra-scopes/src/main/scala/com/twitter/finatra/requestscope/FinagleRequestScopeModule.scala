package com.twitter.finatra.requestscope

import com.twitter.finatra.guice.GuiceModule

object FinagleRequestScopeModule extends GuiceModule {

  override def configure() {
    val finagleScope = new FinagleRequestScope()

    bindScope(classOf[FinagleRequestScoped], finagleScope)
    bind[FinagleRequestScope].toInstance(finagleScope)

    bind[PathURL]
      .toProvider[UnseededFinagleScopeProvider[PathURL]]
      .in[FinagleRequestScoped]
  }
}
