package com.twitter.finatra.kafkastreams.integration.punctuator

import com.twitter.finagle.stats.StatsReceiver
import org.apache.kafka.streams.processor.{ProcessorContext, Punctuator}

class HeartBeatPunctuator(processorContext: ProcessorContext, statsReceiver: StatsReceiver)
    extends Punctuator {
  def punctuate(timestampMillis: Long): Unit = {
    punctuateCounter.incr()
    processorContext.forward(timestampMillis, timestampMillis)
  }

  private val punctuateCounter = statsReceiver.counter("punctuate")
}
