package com.twitter.inject.requestscope

import com.google.inject.Provider

class UnseededFinagleScopeProvider[T] extends Provider[T] {
  override def get: T = {
    throw new IllegalStateException(
      "If you got here then it means that" +
        " your code asked for scoped object which should have been" +
        " explicitly seeded in this scope by calling" +
        " FinagleRequestScope.seed()."
    )
  }
}
