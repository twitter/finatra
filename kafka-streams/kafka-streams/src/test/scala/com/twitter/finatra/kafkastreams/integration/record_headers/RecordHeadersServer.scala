package com.twitter.finatra.kafkastreams.integration.record_headers

import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.{Consumed, Produced, Transformer, TransformerSupplier}
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.{KeyValue, StreamsBuilder}

class RecordHeadersServer(filterValue: Int) extends KafkaStreamsTwitterServer {
  private[this] val TransformerSupplier =
    new TransformerSupplier[String, String, KeyValue[String, String]] {
      override def get(): Transformer[String, String, KeyValue[String, String]] =
        new HeaderFilter[String, String]()
    }

  private[this] val ShouldForwardHeaderKey = "should-forward"

  override val name = "filter-by-recordheaders"

  override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
    builder.asScala
      .stream[String, String]("RecordHeadersTopic")(Consumed.`with`(Serdes.String, Serdes.String))
      .transform(TransformerSupplier)
      .to("RecordHeadersOutputTopic")(Produced.`with`(Serdes.String, Serdes.String))
  }

  /**
   * Simple filter which will filter out records if they do not contain a header
   * with a key of 'should-forward' and a the value passed in via the RecordHeadersServer
   * 'filterValue' parameter
   */
  class HeaderFilter[K, V] extends Transformer[K, V, KeyValue[K, V]] {
    private[this] var processorContext: ProcessorContext = _

    override def init(
      processorContext: ProcessorContext
    ): Unit = this.processorContext = processorContext

    override def transform(k: K, v: V): KeyValue[K, V] = {
      Option(processorContext.headers().lastHeader(ShouldForwardHeaderKey)).map {
        shouldForwardHeaderValue =>
          if (shouldForwardHeaderValue.value().sameElements(Array(filterValue.toByte))) {
            new KeyValue(k, v)
          } else {
            null
          }
      }.orNull
    }

    override def close(): Unit = ()
  }
}
