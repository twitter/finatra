package com.twitter.finatra.thrift.tests;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import scala.reflect.ClassTag$;

import com.google.inject.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.twitter.finagle.stats.InMemoryStatsReceiver;
import com.twitter.finatra.thrift.EmbeddedThriftServer;
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingJavaThriftServer;
import com.twitter.inject.server.PortUtils;
import com.twitter.util.Await;
import com.twitter.util.Duration;
import com.twitter.util.Future;

public class MultiJavaServerDarkTrafficFeatureTest extends Assert {
  private static final String THRIFT_CLIENT_ID = "client123";

  private static final EmbeddedThriftServer DARK_THRIFT_SERVER =
      new EmbeddedThriftServer(
          new DoEverythingJavaThriftServer("dark-thrift-server"),
          Collections.singletonMap("magicNum", "57"),
          Stage.DEVELOPMENT,
          true);
  private static final EmbeddedThriftServer LIVE_THRIFT_SERVER =
      new EmbeddedThriftServer(
          new DoEverythingJavaThriftServer("live-thrift-server"),
          Collections.unmodifiableMap(darkServerFlags()),
          Stage.DEVELOPMENT,
          true);
  private static final com.twitter.doeverything.thriftjava.DoEverything.ServiceIface THRIFT_CLIENT =
      LIVE_THRIFT_SERVER.thriftClient(
          THRIFT_CLIENT_ID,
          ClassTag$.MODULE$.apply(
              com.twitter.doeverything.thriftjava.DoEverything.ServiceIface.class));

  @AfterClass
  public static void tearDown() throws Exception {
    LIVE_THRIFT_SERVER.close();
    DARK_THRIFT_SERVER.close();
  }

  @Before
  public void beforeEach() {
    ((InMemoryStatsReceiver) LIVE_THRIFT_SERVER.statsReceiver()).clear();
    ((InMemoryStatsReceiver) DARK_THRIFT_SERVER.statsReceiver()).clear();
  }

  private static Map<String, String> darkServerFlags() {
    Map<String, String> flags = new HashMap<>();
    flags.put("magicNum", "137");
    flags.put(
        "thrift.dark.service.dest",
        "/$/inet/" + PortUtils.loopbackAddress() + "/" + DARK_THRIFT_SERVER.thriftPort());
    flags.put(
        "thrift.dark.service.clientId", THRIFT_CLIENT_ID);

    return flags;
  }

  /** magicNum is forwarded */
  @Test
  public void magicNum() throws Exception {
    assertEquals("137", await(THRIFT_CLIENT.magicNum()));

    // give a chance for the stat to be recorded on the live service
    LIVE_THRIFT_SERVER.assertHealthy(true);

    // darkTrafficFilter stats
    LIVE_THRIFT_SERVER.inMemoryStats().counters().expect(
        "dark_traffic_filter/forwarded", 1);
    assertFalse(LIVE_THRIFT_SERVER.inMemoryStats().counters().get(
        "dark_traffic_filter/skipped").isDefined());

    // give a chance for the stat to be recorded on the dark service
    DARK_THRIFT_SERVER.assertHealthy(true);
    // "dark" service stats
    DARK_THRIFT_SERVER.inMemoryStats().counters().expect(
        "srv/thrift/requests", 1);
    DARK_THRIFT_SERVER.inMemoryStats().counters().expect(
        "srv/thrift/success", 1);
  }

  /** echo is forwarded */
  @Test
  public void echo() throws Exception {
    assertEquals("words", await(THRIFT_CLIENT.echo("words")));

    // give a chance for the stat to be recorded on the live service
    LIVE_THRIFT_SERVER.assertHealthy(true);

    // darkTrafficFilter stats
    LIVE_THRIFT_SERVER.inMemoryStats().counters().expect(
        "dark_traffic_filter/forwarded", 1);
    assertFalse(LIVE_THRIFT_SERVER.inMemoryStats().counters().get(
        "dark_traffic_filter/skipped").isDefined());

    // give a chance for the stat to be recorded on the dark service
    DARK_THRIFT_SERVER.assertHealthy(true);
    // "dark" service stats
    DARK_THRIFT_SERVER.inMemoryStats().counters().expect(
        "srv/thrift/requests", 1);
    DARK_THRIFT_SERVER.inMemoryStats().counters().expect(
        "srv/thrift/success", 1);
  }

  private <T> T await(Future<T> future) throws Exception {
    return Await.result(future, Duration.fromSeconds(2));
  }
}
