package com.twitter.inject.thrift.modules

import com.twitter.finagle.{Filter, ThriftMux}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.inject.thrift.filters.JavaDarkTrafficFilter

/**
 * A [[com.twitter.inject.TwitterModule]] which configures and binds a [[JavaDarkTrafficFilter]] to the object graph.
 *
 * @note This is only applicable with code that uses generated Java classes as it expects to configure
 *       the [[JavaDarkTrafficFilter]] over a [[com.twitter.finagle.Service]] that is generated from
 *       Finagle via generated Java code. Users of generated Scala code should use the
 *       [[ReqRepDarkTrafficFilterModule]].
 */
abstract class JavaDarkTrafficFilterModule
  extends DarkTrafficFilterModuleTrait {

  /**
   * Function to determine if the request should be "sampled", e.g.
   * sent to the dark service.
   *
   * @param injector the [[com.twitter.inject.Injector]] for use in determining if a given request
   *                 should be forwarded or not.
   */
  protected def enableSampling(injector: Injector): com.twitter.util.Function[Array[Byte], Boolean]

  override protected def newFilter(
    dest: String,
    client: ThriftMux.Client,
    injector: Injector,
    stats: StatsReceiver
  ): Filter.TypeAgnostic = {
    new JavaDarkTrafficFilter(
      client.newService(dest, label),
      enableSampling(injector),
      forwardAfterService,
      stats
    )
  }

}
