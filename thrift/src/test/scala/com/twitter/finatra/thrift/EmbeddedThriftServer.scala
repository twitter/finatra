package com.twitter.finatra.thrift

import com.google.inject.Stage
import com.twitter.inject.server.PortUtils._
import com.twitter.inject.server.{EmbeddedTwitterServer, PortUtils, Ports}
import java.lang.annotation.Annotation
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

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
  failOnLintViolation: Boolean = false
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
      failOnLintViolation = failOnLintViolation
    )
    with ThriftClient {

  /* Additional Constructors */

  def this(twitterServer: Ports, flags: java.util.Map[String, String], stage: Stage) = {
    this(twitterServer, flags = flags.asScala.toMap, stage = stage)
  }

  /* Overrides */

  /**
   * Bind an instance of type [T] to the object graph of the underlying thrift server.
   * This will REPLACE any previously bound instance of the given type.
   *
   * @param instance - to bind instance.
   * @tparam T - type of the instance to bind.
   * @return this [[EmbeddedThriftServer]].
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  override def bind[T: TypeTag](instance: T): EmbeddedThriftServer = {
    bindInstance[T](instance)
    this
  }

  /**
   * Bind an instance of type [T] annotated with Annotation type [A] to the object
   * graph of the underlying thrift server. This will REPLACE any previously bound instance of
   * the given type bound with the given annotation type.
   *
   * @param instance - to bind instance.
   * @tparam T - type of the instance to bind.
   * @tparam A - type of the Annotation used to bind the instance.
   * @return this [[EmbeddedThriftServer]].
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  override def bind[T: TypeTag, A <: Annotation: TypeTag](instance: T): EmbeddedThriftServer = {
    bindInstance[T, A](instance)
    this
  }

  /**
   * Bind an instance of type [T] annotated with the given Annotation value to the object
   * graph of the underlying thrift server. This will REPLACE any previously bound instance of
   * the given type bound with the given annotation.
   *
   * @param annotation - [[java.lang.annotation.Annotation]] instance value
   * @param instance - to bind instance.
   * @tparam T - type of the instance to bind.
   * @return this [[EmbeddedThriftServer]].
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  override def bind[T: TypeTag](annotation: Annotation, instance: T): EmbeddedThriftServer = {
    bindInstance[T](annotation, instance)
    this
  }

  /* Public */

  def thriftPort: Int = {
    start()
    twitterServer.thriftPort.get
  }

  def thriftHostAndPort: String = {
    PortUtils.loopbackAddressForPort(thriftPort)
  }
}
