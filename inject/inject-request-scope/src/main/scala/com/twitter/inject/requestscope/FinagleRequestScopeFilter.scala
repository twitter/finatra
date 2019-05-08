package com.twitter.inject.requestscope

import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future
import javax.inject.Inject

object FinagleRequestScopeFilter {
  /**
   * A [[com.twitter.finagle.Filter.TypeAgnostic]] version of the [[FinagleRequestScopeFilter]]
   * @param finagleRequestScope the custom [[com.google.inject.Scope]] to use.
   */
  class TypeAgnostic @Inject()(
    finagleRequestScope: FinagleRequestScope
  ) extends Filter.TypeAgnostic {
    override def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] =
      new FinagleRequestScopeFilter(finagleRequestScope)
  }
}

/**
 * A [[com.twitter.finagle.Filter]] which is responsible for preparing scoping of the custom
 * [[com.twitter.inject.requestscope.FinagleRequestScope]] around this Filter's underlying
 * [[com.twitter.finagle.Service]].
 *
 * @param finagleRequestScope the custom [[com.google.inject.Scope]] to use.
 *
 * @note if you wish to use a [[com.twitter.finagle.Filter.TypeAgnostic]] version of this
 *       Filter, please see the [[com.twitter.inject.requestscope.FinagleRequestScopeFilter.TypeAgnostic]]
 */
class FinagleRequestScopeFilter[Req, Rep] @Inject()(
  finagleRequestScope: FinagleRequestScope
) extends Filter[Req, Rep, Req, Rep] {

  def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    finagleRequestScope.let {
      service.apply(request)
    }
  }
}
