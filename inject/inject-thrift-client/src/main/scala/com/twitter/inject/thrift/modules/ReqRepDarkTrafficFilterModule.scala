package com.twitter.inject.thrift.modules

import com.twitter.finagle.Filter
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.service.Filterable
import com.twitter.finagle.thrift.service.ReqRepServicePerEndpointBuilder
import com.twitter.inject.Injector
import com.twitter.inject.thrift.filters.DarkTrafficFilter
import scala.reflect.ClassTag

/**
 * A [[com.twitter.inject.TwitterModule]] which configures and binds a [[DarkTrafficFilter]] to the object graph.
 *
 * @note This [[DarkTrafficFilter]] module is to be used with `Controllers` which are constructed by
 *       extending `Controller(GeneratedThriftService)`.
 * @note This is only applicable in Scala as it uses generated Scala classes and expects to configure
 *       the [[DarkTrafficFilter]] over a [[com.twitter.finagle.Service]] that is generated from
 *       Finagle via generated Scala code. Users of generated Java code should use the
 *       [[JavaDarkTrafficFilterModule]].
 */
abstract class ReqRepDarkTrafficFilterModule[MethodIface <: Filterable[MethodIface]: ClassTag](
  implicit serviceBuilder: ReqRepServicePerEndpointBuilder[MethodIface])
    extends DarkTrafficFilterModuleTrait {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in determining if a given request
   *                 should be forwarded or not.
   */
  protected def enableSampling(injector: Injector): Any => Boolean

  protected def newFilter(
    dest: String,
    client: ThriftMux.Client,
    injector: Injector,
    stats: StatsReceiver
  ): Filter.TypeAgnostic = {
    new DarkTrafficFilter[MethodIface](
      client.servicePerEndpoint[MethodIface](dest, label),
      enableSampling(injector),
      forwardAfterService,
      stats,
      lookupByMethod = true
    )
  }
}
