package com.twitter.finatra.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.inject.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twitter.inject.server.EmbeddedTwitterServer;
import com.twitter.util.Await;
import com.twitter.util.Duration;

public class ExampleTwitterServerFeatureTest extends Assert {
  private static final int SUBSCRIBER_MAX_READ = 3;
  private static final TestQueue TEST_QUEUE = new TestQueue();

  private static final EmbeddedTwitterServer SERVER = setup();

  private static EmbeddedTwitterServer setup() {
    Map<String, String> flags = new HashMap<>();
    flags.put("subscriber.max.read", String.valueOf(SUBSCRIBER_MAX_READ));
    return new EmbeddedTwitterServer(
        new ExampleTwitterServer(),
        flags,
        Stage.DEVELOPMENT
    ).bindClass(Queue.class, TEST_QUEUE);
  }

  @BeforeClass
  public static void before() throws InterruptedException {
    SERVER.start();
    SERVER.assertHealthy(true);

    // The server can be marked as started before the TestQueue is written to, resulting
    // in flaky test execution. We wait for the first execution before allowing tests to continue.
    TEST_QUEUE.firstWriteLatch.await(1, TimeUnit.SECONDS);
  }

  @AfterClass
  public static void tearDown() {
    SERVER.close();
  }

  @Test
  public void queueTest() throws Exception {
    assertEquals(TEST_QUEUE.addCounter.get(), 5);
    // wait for underlying server to finish main method which should happen once Subscriber returns
    // Subscriber should exit once it hits the SUBSCRIBER_MAX_READ value.
    Await.result(SERVER.mainResult(), Duration.fromSeconds(10));
    Subscriber subscriber = SERVER.injector().instance(Subscriber.class);
    assertEquals(subscriber.readCount(), SUBSCRIBER_MAX_READ);
  }
}
