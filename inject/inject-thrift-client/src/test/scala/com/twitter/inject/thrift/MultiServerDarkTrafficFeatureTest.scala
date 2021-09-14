package com.twitter.inject.thrift

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.thrift.{EmbeddedThriftServer, ThriftTest}
import com.twitter.inject.Test
import com.twitter.inject.server.PortUtils
import com.twitter.inject.thrift.integration.DarkTrafficThriftServer
import com.twitter.test.thriftscala.EchoService

class MultiServerDarkTrafficFeatureTest extends Test with ThriftTest {

  private[this] val darkEchoThriftServer =
    new EmbeddedThriftServer(new DarkTrafficThriftServer, disableTestLogging = true)
  private[this] val liveEchoThriftServer = new EmbeddedThriftServer(
    new DarkTrafficThriftServer,
    flags = Map(
      "thrift.dark.service.dest" -> s"/$$/inet/${PortUtils.loopbackAddress}/${darkEchoThriftServer.thriftPort()}",
      "thrift.dark.service.clientId" -> "client123"
    ),
    disableTestLogging = true
  )

  private[this] lazy val client123: EchoService.MethodPerEndpoint =
    liveEchoThriftServer.thriftClient[EchoService.MethodPerEndpoint](clientId = "client123")

  // See DoEverythingThriftServerDarkTrafficFilterModule#enableSampling

  test("echo is forwarded") {
    await(client123.echo("12345")) should equal("12345")

    // service stats
    liveEchoThriftServer.inMemoryStats.counters.assert("per_method_stats/echo/success", 1)

    // darkTrafficFilter stats
    liveEchoThriftServer.inMemoryStats.counters.assert("dark_traffic_filter/forwarded", 1)
    liveEchoThriftServer.inMemoryStats.counters.get("dark_traffic_filter/skipped") should be(None)

    darkEchoThriftServer
      .assertHealthy() // give a chance for the stat to be recorded on the dark service
    // "dark" service stats
    darkEchoThriftServer.inMemoryStats.counters.waitFor("per_method_stats/echo/success", 1)
  }

  test("setTimesToEcho is not forwarded") {
    await(client123.setTimesToEcho(5)) should equal(5)

    // service stats
    liveEchoThriftServer.inMemoryStats.counters.assert("per_method_stats/setTimesToEcho/success", 1)
    // darkTrafficFilter stats
    liveEchoThriftServer.inMemoryStats.counters.get("dark_traffic_filter/forwarded") should be(None)
    liveEchoThriftServer.inMemoryStats.counters.assert("dark_traffic_filter/skipped", 1)

    // "dark" service stats
    // no invocations on the doEverythingThriftServer1 as nothing is forwarded
    darkEchoThriftServer.inMemoryStats.counters
      .get("per_method_stats/setTimesToEcho/success") should be(None)
  }

  override protected def beforeEach(): Unit = {
    darkEchoThriftServer.statsReceiver.asInstanceOf[InMemoryStatsReceiver].clear()
    liveEchoThriftServer.statsReceiver.asInstanceOf[InMemoryStatsReceiver].clear()
  }

  override def afterAll(): Unit = {
    await(client123.asClosable.close())
    darkEchoThriftServer.close()
    liveEchoThriftServer.close()
    super.afterAll()
  }
}
