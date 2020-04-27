package com.twitter.finatra.example;

import javax.inject.Inject;

import com.twitter.calculator.thriftjava.Calculator;
import com.twitter.util.Future;
import com.twitter.util.logging.Logger;

public class CalculatorController implements Calculator.ServiceIface {
  private static final Logger LOG = Logger.apply(CalculatorController.class);

  private final Mode mode;

  @Inject
  CalculatorController(Mode mode) {
    this.mode = mode;
  }

  @Override
  public Future<Integer> increment(int a) {
    LOG.info("Current calculator mode: " + mode);
    return Future.value(a + 1);
  }

  @Override
  public Future<Integer> addNumbers(int a, int b) {
    LOG.info("Current calculator mode: " + mode);
    return Future.value(a + b);
  }

  @Override
  public Future<String> addStrings(String a, String b) {
    LOG.info("Current calculator mode: " + mode);
    Integer total = Integer.parseInt(a) + Integer.parseInt(b);
    return Future.value(total.toString());
  }
}
