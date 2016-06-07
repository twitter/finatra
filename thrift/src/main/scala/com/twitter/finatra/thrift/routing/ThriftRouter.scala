package com.twitter.finatra.thrift.routing

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.internal.ThriftMethodService
import com.twitter.inject.{Injector, Logging}
import com.twitter.scrooge.{ThriftMethod, ThriftService, ToThriftService}
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.{Map => MutableMap}

@Singleton
class ThriftRouter @Inject()(
  statsReceiver: StatsReceiver,
  injector: Injector)
  extends Logging {

  private var filterChain = ThriftFilter.Identity
  private var done = false
  private[finatra] var name: String = ""
  private[finatra] var filteredService: ThriftService = _
  private[finatra] val methods = MutableMap[ThriftMethod, ThriftMethodService[_, _]]()

  /* Public */

  /** Add global filter used for all requests */
  def filter[FilterType <: ThriftFilter : Manifest]: ThriftRouter = {
    filter(injector.instance[FilterType])
  }

  /** Add global filter used for all requests */
  def filter(clazz: Class[_ <: ThriftFilter]): ThriftRouter = {
    filter(injector.instance(clazz))
  }

  /** Add global filter used for all requests */
  def filter(filter: ThriftFilter): ThriftRouter = {
    assert(filteredService == null, "'filter' must be called before 'add'.")
    filterChain = filterChain andThen filter
    this
  }

  def add[C <: Controller with ToThriftService : Manifest]: ThriftRouter = {
    val controller = injector.instance[C]
    for (m <- controller.methods) {
      m.setFilter(filterChain)
      methods += (m.method -> m)
    }
    info("Adding methods\n" + (controller.methods.map(method => s"${controller.getClass.getSimpleName}.${method.name}") mkString "\n"))
    if (controller.methods.isEmpty) error(s"${controller.getClass.getCanonicalName} contains no methods!")
    filteredService = controller.toThriftService

    assert(!done, "ThriftRouter#add cannot be called multiple times, as we don't currently support serving multiple thrift services.")
    done = true
    this
  }

  /* Private */

  private[finatra] def serviceName(name: String): ThriftRouter = {
    this.name = name
    this
  }
}
