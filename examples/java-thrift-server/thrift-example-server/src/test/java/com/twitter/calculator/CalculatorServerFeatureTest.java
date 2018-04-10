package com.twitter.calculator;

import java.util.Collections;

import scala.reflect.ClassTag$;

import com.google.inject.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import com.twitter.calculator.thriftjava.Calculator;
import com.twitter.finatra.thrift.EmbeddedThriftServer;
import com.twitter.util.Await;

public class CalculatorServerFeatureTest extends Assert {

  private static final EmbeddedThriftServer SERVER = setup();
  private static final Calculator.ServiceIface THRIFT_CLIENT =
      SERVER.thriftClient(
          "client123",
          ClassTag$.MODULE$.apply(Calculator.ServiceIface.class));

  public static EmbeddedThriftServer setup() {
      EmbeddedThriftServer server =
          new EmbeddedThriftServer(
              new CalculatorServer(),
              Collections.emptyMap(),
              Stage.DEVELOPMENT);
      server.bindClass(Mode.class, Mode.ReversePolarNotation);
      return server;
  }

  @AfterClass
  public static void tearDown() throws Exception {
      SERVER.close();
  }

  /** test increment endpoint */
  @Test
  public void testIncrementEndpoint() throws Exception {
      assertEquals(4, Await.<Integer>result(THRIFT_CLIENT.increment(3)).intValue());
  }

  /** test add numbers endpoint */
  @Test
  public void testAddNumbersEndpoint() throws Exception {
      assertEquals(10, Await.<Integer>result(THRIFT_CLIENT.addNumbers(3, 7)).intValue());
  }

  /** test add strings endpoint */
  @Test
  public void testAddStringsEndpoint() throws Exception {
      assertEquals("10", Await.result(THRIFT_CLIENT.addStrings("3", "7")));
  }
}
