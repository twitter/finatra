package com.twitter.finatra.example;

import java.util.Collections;

import com.google.inject.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twitter.inject.server.EmbeddedTwitterServer;

public class ExampleTwitterServerStartupTest extends Assert {

  private static final EmbeddedTwitterServer SERVER =
      new EmbeddedTwitterServer(
          new ExampleTwitterServer(),
          Collections.emptyMap(),
          Stage.PRODUCTION
      );

  @BeforeClass
  public static void setup() {
    SERVER.start();
  }

  @AfterClass
  public static void tearDown() {
    SERVER.close();
  }

  @Test
  public void startupTest() throws Exception {
    SERVER.assertHealthy(true);
  }
}
