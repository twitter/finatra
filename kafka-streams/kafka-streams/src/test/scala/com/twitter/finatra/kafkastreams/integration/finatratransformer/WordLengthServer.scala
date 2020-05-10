package com.twitter.finatra.kafkastreams.integration.finatratransformer

import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.integration.finatratransformer.WordLengthServer._
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}

object WordLengthServer {
  val timerStoreName = "timers"
  val stringsAndInputsTopic = "strings-and-inputs"
  val StringsAndOutputsTopic = "strings-and-outputs"
}

class WordLengthServer extends KafkaStreamsTwitterServer {

  override protected def configureKafkaStreams(streamsBuilder: StreamsBuilder): Unit = {

    kafkaStreamsBuilder.addStateStore(
      FinatraTransformer.timerStore(timerStoreName, Serdes.String(), streamsStatsReceiver))

    val transformerSupplier = () => new WordLengthFinatraTransformer(statsReceiver, timerStoreName)

    streamsBuilder.asScala
      .stream(stringsAndInputsTopic)(
        Consumed.`with`(Serdes.String(), Serdes.String())
      ).transform(transformerSupplier, timerStoreName)
      .to(StringsAndOutputsTopic)(Produced.`with`(Serdes.String(), Serdes.String()))
  }
}
