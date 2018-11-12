package com.twitter.finatra.thrift.tests

import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.thrift.{ThriftTest, EmbeddedThriftServer}
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingThriftServer
import com.twitter.inject.Test
import com.twitter.inject.server.PortUtils
import com.twitter.util.Future

class MultiServerDarkTrafficFeatureTest
  extends Test
  with ThriftTest {

  private[this] val darkDoEverythingThriftServer = new EmbeddedThriftServer(new DoEverythingThriftServer)
  private[this] val liveDoEverythingThriftServer = new EmbeddedThriftServer(
    new DoEverythingThriftServer,
    flags = Map(
      "thrift.dark.service.dest" -> s"/$$/inet/${PortUtils.loopbackAddress}/${darkDoEverythingThriftServer.thriftPort()}",
      "thrift.dark.service.clientId" -> "client123"
    )
  )

  private[this] lazy val client123: DoEverything[Future] =
    liveDoEverythingThriftServer.thriftClient[DoEverything[Future]](clientId = "client123")

  // See DoEverythingThriftServerDarkTrafficFilterModule#enableSampling

  test("magicNum is forwarded") {
    await(client123.magicNum()) should equal("26")

    // service stats
    liveDoEverythingThriftServer.assertCounter("per_method_stats/magicNum/success", 1)

    // darkTrafficFilter stats
    liveDoEverythingThriftServer.assertCounter("dark_traffic_filter/forwarded", 1)
    liveDoEverythingThriftServer.assertCounter("dark_traffic_filter/skipped", 0)

    darkDoEverythingThriftServer
      .assertHealthy() // give a chance for the stat to be recorded on the dark service
    // "dark" service stats
    darkDoEverythingThriftServer.assertCounter("per_method_stats/magicNum/success", 1)
  }

  test("uppercase not forwarded") {
    await(client123.uppercase("hello")) should equal("HELLO")

    // service stats
    liveDoEverythingThriftServer.assertCounter("per_method_stats/uppercase/success", 1)
    // darkTrafficFilter stats
    liveDoEverythingThriftServer.assertCounter("dark_traffic_filter/forwarded", 0)
    liveDoEverythingThriftServer.assertCounter("dark_traffic_filter/skipped", 1)

    // "dark" service stats
    // no invocations on the doEverythingThriftServer1 as nothing is forwarded
    darkDoEverythingThriftServer.assertCounter("per_method_stats/uppercase/success", 0)
  }

  test("echo is forwarded") {
    await(client123.echo("words")) should equal("words")

    // service stats
    liveDoEverythingThriftServer.assertCounter("per_method_stats/echo/success", 1)

    // darkTrafficFilter stats
    liveDoEverythingThriftServer.assertCounter("dark_traffic_filter/forwarded", 1)
    liveDoEverythingThriftServer.assertCounter("dark_traffic_filter/skipped", 0)

    darkDoEverythingThriftServer
      .assertHealthy() // give a chance for the stat to be recorded on the dark service
    // "dark" service stats
    darkDoEverythingThriftServer.assertCounter("per_method_stats/echo/success", 1)
  }

  test("moreThanTwentyTwoArgs is not forwarded") {
    await(
      client123.moreThanTwentyTwoArgs(
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
        "eleven",
        "twelve",
        "thirteen",
        "fourteen",
        "fifteen",
        "sixteen",
        "seventeen",
        "eighteen",
        "nineteen",
        "twenty",
        "twentyone",
        "twentytwo",
        "twentythree"
      )
    ) should equal("handled")

    // service stats
    liveDoEverythingThriftServer.assertCounter("per_method_stats/moreThanTwentyTwoArgs/success", 1)
    // darkTrafficFilter stats
    liveDoEverythingThriftServer.assertCounter("dark_traffic_filter/forwarded", 0)
    liveDoEverythingThriftServer.assertCounter("dark_traffic_filter/skipped", 1)

    // "dark" service stats
    // no invocations on the doEverythingThriftServer1 as nothing is forwarded
    darkDoEverythingThriftServer.assertCounter("per_method_stats/moreThanTwentyTwoArgs/success", 0)
  }

  override protected def beforeEach(): Unit = {
    darkDoEverythingThriftServer.statsReceiver.asInstanceOf[InMemoryStatsReceiver].clear()
    liveDoEverythingThriftServer.statsReceiver.asInstanceOf[InMemoryStatsReceiver].clear()
  }

  override def afterAll(): Unit = {
    await(client123.asClosable.close())
    darkDoEverythingThriftServer.close()
    liveDoEverythingThriftServer.close()
    super.afterAll()
  }
}
