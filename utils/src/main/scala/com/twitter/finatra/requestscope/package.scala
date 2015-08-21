package com.twitter.finatra

import com.twitter.finagle.httpx.{Request, Response}

package object requestscope {

  @deprecated("com.twitter.inject.requestscope.FinagleRequestScopeFilter", "")
  type FinagleRequestScopeFilter = com.twitter.inject.requestscope.FinagleRequestScopeFilter[Request, Response]

  @deprecated("com.twitter.inject.requestscope.UnseededFinagleScopeProvider", "")
  type UnseededFinagleScopeProvider[T] = com.twitter.inject.requestscope.UnseededFinagleScopeProvider[T]

  @deprecated("com.twitter.inject.requestscope.FinagleRequestScope", "")
  type FinagleRequestScope = com.twitter.inject.requestscope.FinagleRequestScope

  @deprecated("com.twitter.inject.requestscope.RequestScopeBinding", "")
  type RequestScopeBinding = com.twitter.inject.requestscope.RequestScopeBinding
}
