package com.twitter.finatra.thrift

import com.twitter.finagle.Filter.TypeAgnostic
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.thrift.codegen.MethodFilters
import com.twitter.inject.{Injector, Logging}
import com.twitter.scrooge.{ToThriftService, ThriftService}
import javax.inject.{Inject, Singleton}

@Singleton
class ThriftRouter @Inject()(
  statsReceiver: StatsReceiver,
  injector: Injector)
  extends Logging {

  private[finatra] var filterChain: ThriftFilter = ThriftFilter.Identity
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

  /** Add a global TypeAgnostic filter used for all requests */
  def typeAgnosticFilter[FilterType <: TypeAgnostic : Manifest] = {
    filterChain = filterChain andThen ThriftFilter(injector.instance[FilterType])
    this
  }

  // TODO: deprecate this
  def add[T <: ThriftService : Manifest](
    filterFactory: (MethodFilters, T) => ThriftService) {

    addFilteredService(
      filterFactory.apply(
        createMethodFilters,
        injector.instance[T]))
  }

  def add[C <: Controller with ToThriftService : Manifest] = {
    val controller = injector.instance[C]
    for (m <- controller.methods) {
      m.setFilter(filterChain)
      info(s"Added thrift method ${controller.getClass.getCanonicalName}.${m.method.name}")
    }
    if (controller.methods.isEmpty) error(s"${controller.getClass.getCanonicalName} contains no methods!")
    filteredService = controller.toThriftService

    assert(!done, "ThriftRouter#add cannot be called multiple times, as we don't currently support serving multiple thrift services.")
    done = true
  }

  @deprecated("Thrift services should be added with a filter factory.", "since Scrooge 4.x")
  def addUnfiltered[T <: ThriftService : Manifest] = {
    addFilteredService(injector.instance[T])
    this
  }

  /* Private */

  private[finatra] def serviceName(name: String) = {
    this.name = name
    this
  }

  private def addFilteredService[T <: ThriftService](
    thriftService: ThriftService): Unit = {

    assert(!done, "ThriftRouter#add cannot be called multiple times, as we don't currently support serving multiple thrift services.")
    done = true

    filteredService = thriftService
  }

  private def createMethodFilters: MethodFilters = {
    new MethodFilters(statsReceiver.scope(name), filterChain)
  }
}
