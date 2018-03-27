package com.twitter.inject.app.tests;

import java.math.BigDecimal;

public interface PaymentProcessor {

  String processPayment(BigDecimal payment);
}
