package com.twitter.hello.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.inject.Stage;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.inject.server.EmbeddedTwitterServer;

public class HelloWorldServerStartupTest extends Assert {
  private HelloWorldServer underlying = new HelloWorldServer(new ConcurrentLinkedQueue<>());

  private EmbeddedTwitterServer server =
      new EmbeddedTwitterServer(
          underlying,
          Collections.emptyMap(),
          Stage.PRODUCTION);

  @Test
  public void testServerStartup() {
    server.assertHealthy(true);
    server.close();

    ConcurrentLinkedQueue<Integer> queue = underlying.getQueue();
    ConcurrentLinkedQueue<Integer> expected =
        new ConcurrentLinkedQueue<>(Arrays.asList(1, 2, 3, 4, 5));
    // note we do not get to the postMain because the server is blocking and
    // closed outside of the main method. exit blocks are called explicitly by close
    // thus postMains are effectively skipped.
    assertArrayEquals(expected.toArray(), queue.toArray());
  }
}
