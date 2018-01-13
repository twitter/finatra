package com.twitter.finatra.thrift

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.thrift.service.{
  Filterable,
  MethodPerEndpointBuilder,
  ServicePerEndpointBuilder,
  ThriftServiceBuilder
}
import com.twitter.inject.server.{EmbeddedTwitterServer, PortUtils, Ports}
import scala.reflect.ClassTag

trait ThriftClient { self: EmbeddedTwitterServer =>

  /* Abstract */

  /**
   * Underlying Embedded TwitterServer exposed as a [[com.twitter.inject.server.Ports]]
   * @return the underlying TwitterServer as a [[com.twitter.inject.server.Ports]].
   */
  def twitterServer: Ports

  /**
   * The expected flag that sets the external port for serving the underlying Thrift service.
   * @return a String representing the Thrift port flag.
   * @see [[com.twitter.app.Flag]]
   */
  def thriftPortFlag: String = "thrift.port"

  /* Overrides */

  override protected def logStartup() {
    self.logStartup()
    info(s"ExternalThrift -> thrift://$externalThriftHostAndPort\n")
  }

  override protected def combineArgs(): Array[String] = {
    s"-$thriftPortFlag=${PortUtils.ephemeralLoopback}" +: self.combineArgs
  }

  /* Public */

  /**
   * The base ThriftMux.Client to the underlying Embedded TwitterServer.
   */
  def thriftMuxClient(clientId: String): ThriftMux.Client = {
    self.start()
    if (clientId != null && clientId.nonEmpty) {
      ThriftMux.client
        .withStatsReceiver(NullStatsReceiver)
        .withClientId(ClientId(clientId))
    } else {
      ThriftMux.client
        .withStatsReceiver(NullStatsReceiver)
    }
  }

  def thriftMuxClient: ThriftMux.Client = thriftMuxClient(null)

  /**
   * Host and bound external Thrift port combination as a String, e.g., 127.0.0.1:9990.
   */
  lazy val externalThriftHostAndPort: String = PortUtils.loopbackAddressForPort(thriftExternalPort)

  /**
   * Bound external Thrift port for the Embedded TwitterServer.
   * @return the bound external port on which the Embedded TwitterServer is serving the Thrift service.
   */
  def thriftExternalPort: Int = {
    self.start()
    twitterServer.thriftPort.get
  }

  /**
   * Builds a Thrift client to the EmbeddedTwitterServer in the form of the higher-kinded client type
   * or the method-per-endpoint type, e.g.,
   *
   * {{{
   *   val client: MyService[Future] =
   *    server.thriftClient[MyService[Future]](clientId = "client123")
   *
   *   ... or ...
   *
   *   val client: MyService.MethodPerEndpoint =
   *    server.thriftClient[MyService.MethodPerEndpoint](clientId = "client123")
   * }}}
   *
   * @param clientId the client Id to use in creating the thrift client.
   *
   * @return a Finagle Thrift client in the given form.
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id1 Scrooge Finagle Integration - MethodPerEndpoint]]
   */
  def thriftClient[ThriftService: ClassTag](
    clientId: String = null
  ): ThriftService = {
    thriftMuxClient(clientId)
      .build[ThriftService](externalThriftHostAndPort)
  }

  /**
   * Builds a Thrift client to the EmbeddedTwitterServer in the form of a service-per-endpoint or a
   * "Req/Rep" service-per-endpoint.
   *
   * {{{
   *   val client: MyService.ServicePerEndpoint =
   *    server.servicePerEndpoint[MyService.ServicePerEndpoint](clientId = "client123")
   *
   *   ... or ...
   *
   *   val client: [MyService.ReqRepServicePerEndpoint =
   *    server.servicePerEndpoint[MyService.ReqRepServicePerEndpoint](clientId = "client123")
   * }}}
   *
   * @param clientId the client Id to use in creating the thrift client.
   *
   * @return a Finagle Thrift client in the given form.
   * @see [[com.twitter.finagle.thrift.ThriftRichClient.servicePerEndpoint]]
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id2 Scrooge Finagle Integration - ServicePerEndpoint]]
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id3 Scrooge Finagle Integration - ReqRepServicePerEndpoint]]
   */
  def servicePerEndpoint[ServicePerEndpoint <: Filterable[ServicePerEndpoint]](
    clientId: String = null
  )(
    implicit builder: ServicePerEndpointBuilder[ServicePerEndpoint]
  ): ServicePerEndpoint = {
    val label = if (clientId != null) clientId else ""
    thriftMuxClient(clientId)
      .servicePerEndpoint[ServicePerEndpoint](externalThriftHostAndPort, label)
  }

  /**
   * Builds a Thrift client to the EmbeddedTwitterServer in the form of a `MethodPerEndpoint` which
   * wraps a given `ServicePerEndpoint`. Converts the `ServicePerEndpoint` to a
   * `MethodPerEndpoint` interface, e.g., `MyService.MethodPerEndpoint`.
   *
   * {{{
   *  val servicePerEndpoint = MyService.ServicePerEndpoint =
   *    server.servicePerEndpoint[MyService.ServicePerEndpoint](clientId = "client123")
   *  val client: MyService.MethodPerEndpoint =
   *    server.methodPerEndpoint[MyService.ServicePerEndpoint, MyService.MethodPerEndpoint](servicePerEndpoint)
   * }}}
   *
   * This is useful if you want to be able to filter calls to the Thrift service but only want to
   * expose or interact with the RPC-style (method-per-endpoint) client interface.
   *
   * @param servicePerEndpoint the service-per-endpoint to convert to a method-per-endpoint.
   *
   * @return a Finagle Thrift client in the `MyService.MethodPerEndpoint` form of a
   *         method-per-endpoint.
   * @see [[com.twitter.finagle.thrift.ThriftRichClient.methodPerEndpoint]]
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id1 Scrooge Finagle Integration - MethodPerEndpoint]]
   */
  def methodPerEndpoint[ServicePerEndpoint, MethodPerEndpoint](
    servicePerEndpoint: ServicePerEndpoint
  )(
    implicit builder: MethodPerEndpointBuilder[ServicePerEndpoint, MethodPerEndpoint]
  ): MethodPerEndpoint = {
    thriftMuxClient
      .methodPerEndpoint[ServicePerEndpoint, MethodPerEndpoint](servicePerEndpoint)
  }

  /**
   * Builds a Thrift client to the EmbeddedTwitterServer in the higher-kinded form of a
   * method-per-endpoint which wraps a given `ServicePerEndpoint`. Converts the `ServicePerEndpoint`
   * to a higher-kinded method-per-endpoint interface, e.g., `MyService[Future]`.
   *
   * Note: While `MyService.MethodPerEndpoint` and `MyService[Future]` are semantically equivalent,
   * there are differences when it comes to some testing features that have trouble dealing with
   * higher-kinded types (like mocking).
   *
   * Users should prefer to use [[ThriftClient.methodPerEndpoint]] which generates a client
   * in the form of `MyService.MethodPerEndpoint` over this higher-kinded form.
   *
   * {{{
   *  val servicePerEndpoint = MyService.ServicePerEndpoint =
   *    server.servicePerEndpoint[MyService.ServicePerEndpoint](clientId = "client123")
   *  val client: MyService[Future] =
   *    server.thriftClient[MyService.ServicePerEndpoint, MyService[Future]](servicePerEndpoint)
   * }}}
   *
   * This is useful if you want to be able to filter calls to the Thrift service but only want to
   * expose or interact with the RPC-style (method-per-endpoint) client interface.
   *
   * @param servicePerEndpoint the service-per-endpoint to convert to a method-per-endpoint.
   *
   * @return a Finagle Thrift client in the higher-kinded form of a method-per-endpoint.
   * @see [[com.twitter.finagle.thrift.ThriftRichClient.thriftService]]
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id1 Scrooge Finagle Integration - MethodPerEndpoint]]
   */
  @deprecated("Use #methodPerEndpoint", "2018-01-12")
  def thriftClient[ServicePerEndpoint, ThriftService](
    servicePerEndpoint: ServicePerEndpoint
  )(
    implicit builder: ThriftServiceBuilder[ServicePerEndpoint, ThriftService]
  ): ThriftService = {
    thriftMuxClient.thriftService[ServicePerEndpoint, ThriftService](servicePerEndpoint)
  }
}
