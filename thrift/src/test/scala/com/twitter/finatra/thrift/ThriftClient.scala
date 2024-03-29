package com.twitter.finatra.thrift

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.thrift.service.Filterable
import com.twitter.finagle.thrift.service.MethodPerEndpointBuilder
import com.twitter.finagle.thrift.service.ServicePerEndpointBuilder
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.inject.server.PortUtils
import com.twitter.inject.server.Ports
import com.twitter.inject.server.info
import com.twitter.scrooge.AsClosableMethodName
import com.twitter.util.Await
import com.twitter.util.Closable
import com.twitter.util.Duration
import com.twitter.util.Future
import com.twitter.util.Promise
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

  /** Logs the external thrift host and port of the underlying embedded TwitterServer */
  override protected[twitter] def logStartup(): Unit = {
    self.logStartup()
    info(s"ExternalThrift -> thrift://$externalThriftHostAndPort\n", disableLogging)
  }

  /**
   * Adds the [[thriftPortFlag]] with a value pointing to the ephemeral loopback address to
   * the list of flags to be passed to the underlying server.
   * @see [[PortUtils.ephemeralLoopback]]
   *
   * @note this flag is also added in the EmbeddedThriftServer constructor but needs to be added
   * here for when this trait is mixed into an EmbeddedTwitterServer or an EmbeddedHttpServer.
   * The flags are de-duped prior to starting the underlying server.
   */
  override protected[twitter] def combineArgs(): Array[String] = {
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

  /*
   * We need to wait on the external ports to be bound, which can happen after a server
   * is started and marked as healthy.
   */
  private[this] val ready: Promise[Unit] = EmbeddedTwitterServer.isPortReady(
    twitterServer,
    twitterServer.thriftPort.isDefined && twitterServer.thriftPort.get != 0)

  /**
   * Bound external Thrift port for the Embedded TwitterServer.
   * @return the bound external port on which the Embedded TwitterServer is serving the Thrift service.
   */
  def thriftExternalPort: Int = {
    self.start()
    // need to wait until we know the ports are bound
    Await.ready(ready, Duration.fromSeconds(5))
    twitterServer.thriftPort.get
  }

  /**
   * Builds a Thrift client to the EmbeddedTwitterServer in the form of the higher-kinded client type
   * or the method-per-endpoint type, e.g.,
   *
   * {{{
   *   val client: MyService.MethodPerEndpoint =
   *    server.thriftClient[MyService.MethodPerEndpoint](clientId = "client123")
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
   * @see [[com.twitter.finagle.ThriftMux.Client.build(dest: String)]]
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id1 Scrooge Finagle Integration - MethodPerEndpoint]]
   */
  def thriftClient[ThriftService: ClassTag](
    clientId: String = null
  ): ThriftService = ensureClosedOnExit {
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
   * @see [[com.twitter.finagle.ThriftMux.Client.servicePerEndpoint]]
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id2 Scrooge Finagle Integration - ServicePerEndpoint]]
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id3 Scrooge Finagle Integration - ReqRepServicePerEndpoint]]
   */
  def servicePerEndpoint[ServicePerEndpoint <: Filterable[ServicePerEndpoint]](
    clientId: String = null
  )(
    implicit builder: ServicePerEndpointBuilder[ServicePerEndpoint]
  ): ServicePerEndpoint = ensureClosedOnExit {
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
   * @see [[com.twitter.finagle.ThriftMux.Client.methodPerEndpoint]]
   * @see [[https://twitter.github.io/scrooge/Finagle.html#id1 Scrooge Finagle Integration - MethodPerEndpoint]]
   */
  def methodPerEndpoint[ServicePerEndpoint, MethodPerEndpoint](
    servicePerEndpoint: ServicePerEndpoint
  )(
    implicit builder: MethodPerEndpointBuilder[ServicePerEndpoint, MethodPerEndpoint]
  ): MethodPerEndpoint = ensureClosedOnExit {
    ThriftMux.Client
      .methodPerEndpoint[ServicePerEndpoint, MethodPerEndpoint](servicePerEndpoint)
  }

  private[this] def ensureClosedOnExit[T](f: => T): T = {
    val clnt = f
    closeOnExit(asClosableThriftService(clnt))
    clnt
  }

  // copied from our ThriftClientModuleTrait because we can't depend on it here without
  // creating a circular dependency
  private[this] def asClosableThriftService(thriftService: Any): Closable = {
    val close = thriftService match {
      case closable: Closable =>
        closable
      case _ =>
        val asClosableMethodOpt =
          thriftService.getClass.getMethods
            .find(_.getName == AsClosableMethodName)
        asClosableMethodOpt match {
          case Some(method) =>
            try {
              method.invoke(thriftService).asInstanceOf[Closable]
            } catch {
              case _: java.lang.ClassCastException =>
                System.err.println(
                  s"Unable to cast result of ${AsClosableMethodName} invocation to a " +
                    s"${Closable.getClass.getName.dropRight(1)} type."
                )
                Closable.nop
            }
          case _ =>
            System.err.println(
              s"${AsClosableMethodName} not found for instance: ${thriftService.getClass.getName}"
            )
            Closable.nop
        }
    }
    Closable.all(
      Closable.make { _ =>
        info(s"Closing Embedded ThriftClient '$thriftService''", disableLogging)
        Future.Done
      },
      close
    )
  }
}
