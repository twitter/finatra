package com.twitter.inject.app;

import java.math.BigDecimal;

public interface PaymentProcessor {

  String processPayment(BigDecimal payment);
}
