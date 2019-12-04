package com.twitter.inject.thrift.modules

import com.twitter.finagle.{Filter, ThriftMux}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ServiceIfaceBuilder
import com.twitter.finagle.thrift.service.Filterable
import com.twitter.inject.Injector
import com.twitter.inject.thrift.filters.DarkTrafficFilter
import scala.reflect.ClassTag

/**
 * A [[TwitterModule]] which configures and binds a [[DarkTrafficFilter]] to the object graph, for
 * use with [[Controllers]] constructed using the legacy method.
 *
 * @note This [[DarkTrafficFilter]] module is to be used with [[Controllers]] which are constructed using
 *       the deprecated method of extending the `BaseServiceIface` of the generated Thrift service.
 *       For services that construct their Controllers by extending
 *       `Controller(GeneratedThriftService)`, use the [[ReqRepDarkTrafficFilter]] instead
 * @note This is only applicable in Scala as it uses generated Scala classes and expects to configure
 *       the [[DarkTrafficFilter]] over a [[com.twitter.finagle.Service]] that is generated from
 *       Finagle via generated Scala code. Users of generated Java code should use the
 *       [[JavaDarkTrafficFilterModule]].
 */
@deprecated("Use ReqRepDarkTrafficFilterModule", "12-03-2019")
abstract class DarkTrafficFilterModule[ServiceIface <: Filterable[ServiceIface]: ClassTag](
  implicit serviceBuilder: ServiceIfaceBuilder[ServiceIface]
) extends DarkTrafficFilterModuleTrait {

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
    new DarkTrafficFilter[ServiceIface](
      client.newServiceIface[ServiceIface](dest, label),
      enableSampling(injector),
      forwardAfterService,
      stats,
      lookupByMethod = false
    )
  }
}
