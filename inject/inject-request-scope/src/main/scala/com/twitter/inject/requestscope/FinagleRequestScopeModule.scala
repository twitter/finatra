package com.twitter.inject.requestscope

import com.twitter.inject.TwitterModule

object FinagleRequestScopeModule extends TwitterModule {

  override def configure() {
    val finagleScope = new FinagleRequestScope()
    bindScope(classOf[FinagleRequestScoped], finagleScope)
    bind[FinagleRequestScope].toInstance(finagleScope)
  }
}
