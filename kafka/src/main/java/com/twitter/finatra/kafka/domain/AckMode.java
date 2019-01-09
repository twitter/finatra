package com.twitter.finatra.kafka.domain;

public enum AckMode {
    ALL("all"),
    ONE("1"),
    ZERO("0");

    private String value;

    AckMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
