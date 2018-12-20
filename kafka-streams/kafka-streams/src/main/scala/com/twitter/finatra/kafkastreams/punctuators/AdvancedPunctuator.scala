package com.twitter.finatra.kafkastreams.punctuators

import org.apache.kafka.streams.processor.Punctuator

/**
 * A Punctuator that will only only call 'punctuateAdvanced' when the timestamp is greater than the last timestamp.
 *
 * *Note* if you extend this class you probably do not want to override 'punctuate'
 */
trait AdvancedPunctuator extends Punctuator {

  private var lastPunctuateTimeMillis = Long.MinValue

  override def punctuate(timestampMillis: Long): Unit = {
    if (timestampMillis > lastPunctuateTimeMillis) {
      punctuateAdvanced(timestampMillis)
      lastPunctuateTimeMillis = timestampMillis
    }
  }

  /**
   * This will only be called if the timestamp is greater than the previous time
   *
   * @param timestampMillis the timestamp of the punctuate
   */
  def punctuateAdvanced(timestampMillis: Long): Unit
}
