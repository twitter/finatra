package com.twitter.finatra.thrift

/**
 * Provides utilities for writing tests involving [[EmbeddedThriftServer]]
 */
trait ThriftTest {

  def resolverMap(name: String, embeddedThriftServer: EmbeddedThriftServer): (String, String) = {
    ("com.twitter.server.resolverMap", name + "=" + embeddedThriftServer.thriftHostAndPort)
  }
}
