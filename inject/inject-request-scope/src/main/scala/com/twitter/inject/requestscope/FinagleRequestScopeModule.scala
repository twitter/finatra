package com.twitter.inject.requestscope

import com.twitter.inject.TwitterModule

object FinagleRequestScopeModule extends TwitterModule {

  override def configure(): Unit = {
    val finagleRequestScope = new FinagleRequestScope()
    bindScope(classOf[FinagleRequestScoped], finagleRequestScope)
    bind[FinagleRequestScope].toInstance(finagleRequestScope)
  }

  /**  Java-friendly way to access this module as a singleton instance */
  def get(): this.type = this
}
