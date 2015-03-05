package com.twitter.finatra

import com.twitter.finagle.http.{Request, Response}
import com.twitter.inject.requestscope.{FinagleRequestScopeFilter => NewFinagleRequestScopeFilter}
import com.twitter.inject.thrift.{ThriftClientModule => NewThriftClientModule}

package object requestscope {

  @deprecated("com.twitter.inject.requestscope.FinagleRequestScopeFilter")
  type FinagleRequestScopeFilter = com.twitter.inject.requestscope.FinagleRequestScopeFilter[Request, Response]

  @deprecated("com.twitter.inject.requestscope.UnseededFinagleScopeProvider")
  type UnseededFinagleScopeProvider[T] = com.twitter.inject.requestscope.UnseededFinagleScopeProvider[T]

  @deprecated("com.twitter.inject.requestscope.FinagleRequestScope")
  type FinagleRequestScope = com.twitter.inject.requestscope.FinagleRequestScope

  @deprecated("com.twitter.inject.requestscope.RequestScopeBinding")
  type RequestScopeBinding = com.twitter.inject.requestscope.RequestScopeBinding
}
