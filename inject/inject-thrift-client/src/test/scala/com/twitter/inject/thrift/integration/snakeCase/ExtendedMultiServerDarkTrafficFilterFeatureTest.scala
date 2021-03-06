package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.PortUtils

object ExtendedMultiServerDarkTrafficFilterFeatureTest {
  protected val clientId = "client123"
  protected val darkExtendedSnakeCaseThriftServer =
    new EmbeddedThriftServer(new ExtendedSnakeCaseThriftServer, disableTestLogging = true)
  protected val liveExtendedSnakeCaseThriftServer = new EmbeddedThriftServer(
    new ExtendedSnakeCaseThriftServer,
    flags = Map(
      "thrift.dark.service.dest" ->
        s"/$$/inet/${PortUtils.loopbackAddress}/${darkExtendedSnakeCaseThriftServer.thriftPort()}",
      "thrift.dark.service.clientId" ->
        s"$clientId"
    ),
    disableTestLogging = true
  )
}

class ExtendedMultiServerDarkTrafficFilterFeatureTest
    extends AbstractExtendedMultiServerDarkTrafficFilterFeatureTest(
      ExtendedMultiServerDarkTrafficFilterFeatureTest.darkExtendedSnakeCaseThriftServer,
      ExtendedMultiServerDarkTrafficFilterFeatureTest.liveExtendedSnakeCaseThriftServer,
      ExtendedMultiServerDarkTrafficFilterFeatureTest.clientId
    )
