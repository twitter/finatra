package com.twitter.finatra.thrift

import com.twitter.finagle.Filter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.thrift.codegen.MethodFilters
import com.twitter.inject.Injector
import com.twitter.scrooge.ThriftService
import javax.inject.{Inject, Singleton}

@Singleton
class ThriftRouter @Inject()(
  statsReceiver: StatsReceiver,
  injector: Injector) {

  private type ThriftFilter = Filter[ThriftRequest, Any, ThriftRequest, Any]
  private[finatra] var filterChain: ThriftFilter = Filter.identity
  private var done = false
  private[finatra] var name: String = ""
  private[finatra] var filteredService: ThriftService = _

  /* Public */

  /** Add global filter used for all requests */
  def filter[FilterType <: ThriftFilter : Manifest] = {
    filterChain = filterChain andThen injector.instance[FilterType]
    this
  }

  def filter(clazz: Class[_ <: ThriftFilter]) = {
    filterChain = filterChain andThen injector.instance(clazz)
    this
  }

  def add[T <: ThriftService : Manifest](
    filterFactory: (MethodFilters, T) => ThriftService) {

    addFilteredService(
      filterFactory.apply(
        createMethodFilters,
        injector.instance[T]))
  }

  /* Private */

  private[finatra] def serviceName(name: String) = {
    this.name = name
    this
  }

  private def addFilteredService[T <: ThriftService](
    thriftService: ThriftService): Unit = {

    assert(!done, "ThriftRouter#Add cannot be called multiple times, as we don't currently support serving multiple thrift services.")
    done = true

    filteredService = thriftService
  }

  private def createMethodFilters: MethodFilters = {
    new MethodFilters(statsReceiver.scope(name), filterChain)
  }
}
