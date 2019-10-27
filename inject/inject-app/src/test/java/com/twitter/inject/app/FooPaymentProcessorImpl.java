package com.twitter.inject.app;

import java.math.BigDecimal;

public class FooPaymentProcessorImpl implements PaymentProcessor {

  @Override
  public String processPayment(BigDecimal payment) {
    return this.getClass().getSimpleName();
  }
}
