package com.twitter.finatra.kafkastreams.integration.punctuator

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced, Transformer}
import org.apache.kafka.streams.processor.{Cancellable, ProcessorContext, PunctuationType}

class HeartBeatServer extends KafkaStreamsTwitterServer {
  override val name = "heartbeat"

  private val transformerSupplier: () => Transformer[Long, Long, (Long, Long)] = () =>
    new Transformer[Long, Long, (Long, Long)] {
      private val transformCounter = streamsStatsReceiver.counter("transform")

      private var heartBeatPunctuatorCancellable: Cancellable = _

      override def close(): Unit = {
        if (heartBeatPunctuatorCancellable != null) {
          heartBeatPunctuatorCancellable.cancel()
        }
      }

      override def init(processorContext: ProcessorContext): Unit = {
        heartBeatPunctuatorCancellable = processorContext.schedule(
          1.second.inMillis,
          PunctuationType.WALL_CLOCK_TIME,
          new HeartBeatPunctuator(processorContext, streamsStatsReceiver))
      }

      override def transform(k: Long, v: Long): (Long, Long) = {
        transformCounter.incr()
        (k, v)
      }
    }

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder.asScala
      .stream[Long, Long]("input-topic")(Consumed.`with`(ScalaSerdes.Long, ScalaSerdes.Long))
      .transform[Long, Long](transformerSupplier)
      .to("output-topic")(Produced.`with`(ScalaSerdes.Long, ScalaSerdes.Long))
  }
}
