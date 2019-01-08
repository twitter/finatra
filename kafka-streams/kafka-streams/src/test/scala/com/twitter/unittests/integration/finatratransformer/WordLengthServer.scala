package com.twitter.unittests.integration.finatratransformer

import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.streams.transformer.FinatraTransformer
import com.twitter.unittests.integration.finatratransformer.WordLengthServer._
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
      FinatraTransformer.timerStore(timerStoreName, Serdes.String()))

    val transformerSupplier = () =>
      new WordLengthFinatraTransformerV2(statsReceiver, timerStoreName)

    streamsBuilder.asScala
      .stream(stringsAndInputsTopic)(
        Consumed.`with`(Serdes.String(), Serdes.String())
      ).transform(transformerSupplier, timerStoreName)
      .to(StringsAndOutputsTopic)(Produced.`with`(Serdes.String(), Serdes.String()))
  }
}
