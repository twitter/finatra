package com.twitter.finatra.thrift

/**
 * Provides utilities for writing tests involving [[EmbeddedThriftServer]].
 */
trait ThriftTest {

  /**
   * Returns a String tuple of the [[com.twitter.server.resolverMap]] flag name to the
   * host and port of the given [[EmbeddedThriftServer]]. E.g.,
   * {{{
   *   -com.twitter.server.resolverMap=name={EmbeddedThriftServer.thriftHostAndPort}
   * }}}
   * @param name resolver label for the [[EmbeddedThriftServer]].
   * @param embeddedThriftServer [[EmbeddedThriftServer]] for which to add a resolverMap entry.
   * @return String tuple of the [[com.twitter.server.resolverMap]] flag to the name=hostAndPost resolution.
   * @see [[com.twitter.server.resolverMap]]
   * @see [[com.twitter.finagle.Resolver]]
   */
  def resolverMap(name: String, embeddedThriftServer: EmbeddedThriftServer): (String, String) = {
    ("com.twitter.server.resolverMap", name + "=" + embeddedThriftServer.thriftHostAndPort)
  }
}
