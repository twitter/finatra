package com.twitter.finatra.kafkastreams.transformer.lifecycle

trait OnFlush {

  /**
   * Callback method for when you should flush any cached data.
   * This method is typically called prior to a Kafka commit
   */
  protected def onFlush(): Unit = {}
}
