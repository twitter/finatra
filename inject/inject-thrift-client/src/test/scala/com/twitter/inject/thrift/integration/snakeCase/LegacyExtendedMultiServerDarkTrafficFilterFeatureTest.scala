package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.PortUtils

object LegacyExtendedMultiServerDarkTrafficFilterFeatureTest {
  protected val clientId = "client123"
  protected val darkExtendedSnakeCaseThriftServer =
    new EmbeddedThriftServer(new LegacyExtendedSnakeCaseThriftServer, disableTestLogging = true)
  protected val liveExtendedSnakeCaseThriftServer = new EmbeddedThriftServer(
    new LegacyExtendedSnakeCaseThriftServer,
    flags = Map(
      "thrift.dark.service.dest" ->
        s"/$$/inet/${PortUtils.loopbackAddress}/${darkExtendedSnakeCaseThriftServer.thriftPort()}",
      "thrift.dark.service.clientId" ->
        s"$clientId"
    ),
    disableTestLogging = true
  )
}

class LegacyExtendedMultiServerDarkTrafficFilterFeatureTest
    extends AbstractExtendedMultiServerDarkTrafficFilterFeatureTest(
      LegacyExtendedMultiServerDarkTrafficFilterFeatureTest.darkExtendedSnakeCaseThriftServer,
      LegacyExtendedMultiServerDarkTrafficFilterFeatureTest.liveExtendedSnakeCaseThriftServer,
      LegacyExtendedMultiServerDarkTrafficFilterFeatureTest.clientId
    )
