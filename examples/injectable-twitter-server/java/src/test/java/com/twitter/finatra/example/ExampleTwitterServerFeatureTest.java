package com.twitter.finatra.example;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.google.inject.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twitter.inject.server.EmbeddedTwitterServer;

public class ExampleTwitterServerFeatureTest extends Assert {
  private static final TestQueue TEST_QUEUE = new TestQueue();

  private static final EmbeddedTwitterServer SERVER = new EmbeddedTwitterServer(
      new ExampleTwitterServer(),
      Collections.emptyMap(),
      Stage.DEVELOPMENT
  ).bindClass(Queue.class, TEST_QUEUE);

  @BeforeClass
  public static void setup() throws InterruptedException {
    SERVER.start();

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
  }
}
