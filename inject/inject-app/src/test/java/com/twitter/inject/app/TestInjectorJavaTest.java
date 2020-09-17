package com.twitter.inject.app;

import java.math.BigDecimal;
import java.math.MathContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import com.google.inject.Stage;
import com.google.inject.name.Names;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.inject.Injector;
import com.twitter.inject.annotations.Down;
import com.twitter.inject.annotations.Flags;
import com.twitter.inject.modules.InMemoryStatsReceiverModule;
import com.twitter.inject.modules.LoggerModule;
import com.twitter.inject.modules.StatsReceiverModule;

public class TestInjectorJavaTest extends Assert {

  @Test
  public void testJavaConstructors() {
    HashMap<String, String> flags = new HashMap<>();
    flags.put("foo", "bar");
    flags.put("baz", "bus");

    TestInjector.Builder testInjector = TestInjector$.MODULE$.apply();
    testInjector = TestInjector.apply();
    testInjector = TestInjector.apply(LoggerModule.get(), StatsReceiverModule.get());
    testInjector = TestInjector.apply(
            /* modules = */ Arrays.asList(LoggerModule.get(), StatsReceiverModule.get()));
    testInjector = TestInjector.apply(
            /* modules = */ Arrays.asList(LoggerModule.get(), StatsReceiverModule.get()),
            /* flags = */ flags);
    testInjector = TestInjector.apply(
            /* modules = */ Arrays.asList(LoggerModule.get(), StatsReceiverModule.get()),
            /* flags = */ flags,
            /* overrideModules = */ Collections.singletonList(InMemoryStatsReceiverModule.get())
    );
    testInjector = TestInjector.apply(
            /* modules = */ Arrays.asList(LoggerModule.get(), StatsReceiverModule.get()),
            /* flags = */ flags,
            /* overrideModules = */ Collections.singletonList(InMemoryStatsReceiverModule.get()),
            /* stage */ Stage.PRODUCTION
    );
  }

  @Test
  public void testTestInjector() {
    BigDecimal payment = new BigDecimal(12.34, MathContext.DECIMAL32);

    TestInjector.Builder testInjector = TestInjector$.MODULE$.apply();

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

  @Test
  public void testTestInjectorChaining() {
    BigDecimal payment = new BigDecimal(12.34, MathContext.DECIMAL32);

    TestInjector.Builder testInjector = TestInjector.apply(
        LoggerModule.get(),
        StatsReceiverModule.get());

    Injector injector = testInjector
        .bindClass(String.class, "Hello, world")
        .bindClass(Float.class, Flags.named("float.flag"), 42f)
        .bindClass(String.class, Down.class, "Goodbye, world")
        .bindClass(PaymentProcessor.class, BarPaymentProcessorImpl.class)
        .bindClass(
            PaymentProcessor.class, Names.named("processor.impl"), FooPaymentProcessorImpl.class)
        .newInstance();

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

  @Test
  public void testTestInjectorCollections() {
    BigDecimal payment = new BigDecimal(12.34, MathContext.DECIMAL32);

    TestInjector.Builder testInjector = TestInjector.apply(
        Arrays.asList(LoggerModule.get(), StatsReceiverModule.get()));

    Injector injector = testInjector
        .bindClass(String.class, "Hello, world")
        .bindClass(Float.class, Flags.named("float.flag"), 42f)
        .bindClass(String.class, Down.class, "Goodbye, world")
        .bindClass(PaymentProcessor.class, BarPaymentProcessorImpl.class)
        .bindClass(
            PaymentProcessor.class, Names.named("processor.impl"), FooPaymentProcessorImpl.class)
        .create();

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
