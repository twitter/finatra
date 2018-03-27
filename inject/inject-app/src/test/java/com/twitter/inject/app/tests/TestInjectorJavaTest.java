package com.twitter.inject.app.tests;

import java.math.BigDecimal;
import java.math.MathContext;

import com.google.inject.name.Names;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.inject.Injector;
import com.twitter.inject.annotations.Flags;
import com.twitter.inject.app.TestInjector;
import com.twitter.inject.app.TestInjector$;

public class TestInjectorJavaTest extends Assert {

  @Test
  public void testTestInjector() {
    BigDecimal payment = new BigDecimal(12.34, MathContext.DECIMAL32);

    TestInjector testInjector = TestInjector$.MODULE$.apply();

    testInjector.bindClass(String.class, "Hello, world");
    testInjector.bindClass(Float.class, Flags.named("float.flag"), 42f);
    testInjector.bindClass(String.class, Down.class, "Goodbye, world");
    testInjector.bindClass(PaymentProcessor.class, BarPaymentProcessorImpl.class);
    testInjector.bindClass(
        PaymentProcessor.class, Names.named("processor.impl"), FooPaymentProcessorImpl.class);

    Injector injector = testInjector.newInstance();

    assertEquals(injector.instance(String.class), "Hello, world");
    assertTrue(injector.instance(Float.class, Flags.named("float.flag")) == 42f);
    assertEquals(injector.instance(String.class, Down.class), "Goodbye, world");
    assertEquals(injector.instance(
        PaymentProcessor.class).processPayment(payment),
        "BarPaymentProcessorImpl");
    assertEquals(injector.instance(
        PaymentProcessor.class,
        Names.named("processor.impl")).processPayment(payment),
        "FooPaymentProcessorImpl");
  }
}
