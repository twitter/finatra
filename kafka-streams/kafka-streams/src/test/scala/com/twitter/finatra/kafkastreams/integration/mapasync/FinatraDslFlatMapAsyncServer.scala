package com.twitter.finatra.kafkastreams.integration.mapasync

import com.twitter.finatra.kafkastreams.dsl.FinatraDslFlatMapAsync
import com.twitter.finatra.kafkastreams.flushing.FlushingAwareServer
import com.twitter.util.Future
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Produced}
import com.twitter.conversions.DurationOps._
import com.twitter.doeverything.thriftscala.{Answer, Question}
import com.twitter.finatra.kafka.serde.ScalaSerdes

object FinatraDslFlatMapAsyncServer {
  val IncomingTopic = "incoming_topic"
  val FlatMapTopic = "flatMap_topic"
  val FlatMapValuesTopic = "flatMapValues_topic"
  val MapTopic = "map_topic"
  val MapValuesTopic = "mapValues_topic"
  val CommitInterval = 1.minute
}

class FinatraDslFlatMapAsyncServer extends FlushingAwareServer with FinatraDslFlatMapAsync {
  import FinatraDslFlatMapAsyncServer._

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {

    implicit val producedLongString: Produced[Long, String] =
      Produced.`with`(Serdes.Long, Serdes.String)

    implicit val producedLongQuestion: Produced[Long, Question] =
      Produced.`with`(Serdes.Long, ScalaSerdes.Thrift[Question])

    implicit val producedStringString: Produced[String, String] =
      Produced.`with`(Serdes.String, Serdes.String)

    implicit val producedStringAnswer: Produced[String, Answer] =
      Produced.`with`(Serdes.String, ScalaSerdes.Thrift[Answer])

    implicit val consumed: Consumed[Long, Long] = Consumed.`with`(Serdes.Long, Serdes.Long)

    builder.asScala
      .stream(IncomingTopic)
      .flatMapAsync(CommitInterval, 10) { (key: Long, value: Long) =>
        Future.value(Seq(key -> s"$key = $value", key + 1 -> s"${key + 1} = ${value + 1}"))
      }
      .through(FlatMapTopic)
      .flatMapValuesAsync(CommitInterval, 10) { value: String =>
        Future.value(Seq(Question(value), Question(s"$value and more")))
      }
      .through(FlatMapValuesTopic)
      .mapAsync(CommitInterval, 10) { (key: Long, value: Question) =>
        Future.value(key.toString -> value.text)
      }
      .through(MapTopic)
      .mapValuesAsync(CommitInterval, 10) { value: String =>
        Future.value(Answer(value))
      }
      .to(MapValuesTopic)
  }
}
