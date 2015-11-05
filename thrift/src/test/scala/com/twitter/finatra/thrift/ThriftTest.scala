package com.twitter.finatra.thrift

trait ThriftTest {

  def resolverMap(name: String, embeddedThriftServer: EmbeddedThriftServer): (String, String) = {
    ("com.twitter.server.resolverMap", name + "=" + embeddedThriftServer.thriftHostAndPort)
  }
}
