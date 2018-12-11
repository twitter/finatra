package com.twitter.finatra.thrift

import com.google.inject.Stage
import com.twitter.inject.server.PortUtils._
import com.twitter.inject.server.{EmbeddedTwitterServer, PortUtils, Ports}
import com.twitter.util.Duration
import scala.collection.JavaConverters._

/**
 * EmbeddedThriftServer allows a [[com.twitter.server.TwitterServer]] serving thrift endpoints to be started
 * locally (on ephemeral ports), and tested through it's thrift interface.
 *
 * @param twitterServer The twitter server to be started locally for integration testing.
 * @param flags Command line Flags (e.g. "foo"->"bar" will be translated into -foo=bar). See: [[com.twitter.app.Flag]].
 * @param args Extra command line arguments. Could be flags, e.g, -foo=bar or other args, i.e, -Dfoo=bar -Xmx512M, etc.
 * @param waitForWarmup Once the server is started, wait for App warmup to be completed.
 * @param stage [[com.google.inject.Stage]] used to create the server's injector. Since EmbeddedThriftServer is used for testing,
 *              we default to Stage.DEVELOPMENT. This makes it possible to only mock objects that are used in a given test,
 *              at the expense of not checking that the entire object graph is valid. As such, you should always have at
 *              least one Stage.PRODUCTION test for your service (which eagerly creates all classes at startup).
 * @param useSocksProxy Use a tunneled socks proxy for external service discovery/calls (useful for manually run external
 *                      integration tests that connect to external services).
 * @param thriftPortFlag Name of the flag that defines the external thrift port for the server.
 * @param verbose Enable verbose logging during test runs.
 * @param disableTestLogging Disable all logging emitted from the test infrastructure.
 * @param maxStartupTimeSeconds Maximum seconds to wait for embedded server to start. If exceeded an Exception is thrown.
 * @param failOnLintViolation If server startup should fail due (and thus the test) to a detected lint rule issue after startup.
 * @param closeGracePeriod An Optional grace period to use instead of the underlying server's
 *                         `defaultGracePeriod` when closing the underlying server.
 */
class EmbeddedThriftServer(
  override val twitterServer: Ports,
  flags: => Map[String, String] = Map(),
  args: => Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  stage: Stage = Stage.DEVELOPMENT,
  useSocksProxy: Boolean = false,
  override val thriftPortFlag: String = "thrift.port",
  verbose: Boolean = false,
  disableTestLogging: Boolean = false,
  maxStartupTimeSeconds: Int = 60,
  failOnLintViolation: Boolean = false,
  closeGracePeriod: Option[Duration] = None
) extends EmbeddedTwitterServer(
      twitterServer,
      flags + (thriftPortFlag -> ephemeralLoopback),
      args,
      waitForWarmup,
      stage,
      useSocksProxy,
      verbose = verbose,
      disableTestLogging = disableTestLogging,
      maxStartupTimeSeconds = maxStartupTimeSeconds,
      failOnLintViolation = failOnLintViolation,
      closeGracePeriod = closeGracePeriod
    )
    with ThriftClient {

  /* Additional Constructors */

  def this(twitterServer: Ports, flags: java.util.Map[String, String], stage: Stage) =
    this(twitterServer, flags = flags.asScala.toMap, stage = stage)

  def this(twitterServer: Ports, flags: java.util.Map[String, String], stage: Stage, disableTestLogging: Boolean) =
    this(twitterServer, flags = flags.asScala.toMap, stage = stage, disableTestLogging = disableTestLogging)

  /* Public */

  /** The assigned external "thrift" port for the underlying embedded ThriftServer */
  def thriftPort(): Int = {
    start()
    twitterServer.thriftPort.get
  }

  /** A `host:post` String of the loopback and external "thrift" port for the underlying embedded ThriftServer */
  def thriftHostAndPort: String = {
    PortUtils.loopbackAddressForPort(thriftPort())
  }
}
