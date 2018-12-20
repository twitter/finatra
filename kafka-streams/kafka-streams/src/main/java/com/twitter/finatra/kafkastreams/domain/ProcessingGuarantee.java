package com.twitter.finatra.kafkastreams.domain;

public enum ProcessingGuarantee {
  AT_LEAST_ONCE("at_least_once"),
  EXACTLY_ONCE("exactly_once");

  private String value;

  ProcessingGuarantee(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
