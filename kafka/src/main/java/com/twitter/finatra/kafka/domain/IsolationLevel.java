package com.twitter.finatra.kafka.domain;

/**
 * Controls how to read messages written transactionally.
 *
 * If set to read_committed, consumer.poll() will only return transactional messages
 * if they have been committed.
 * If set to read_uncommitted (the default), consumer.poll() will return all messages,
 * even transactional messages which have been aborted.
 *
 * Non-transactional messages will be returned unconditionally in either mode.
 */
public enum IsolationLevel {
    READ_UNCOMMITTED("read_uncommitted"),
    READ_COMMITTED("read_committed");

    private String value;

    IsolationLevel(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
