package com.twitter.finatra.kafka.consumers

import com.twitter.concurrent.AsyncStream
import com.twitter.finatra.kafka.domain.KafkaTopic
import com.twitter.util.{Future, Time}
import org.apache.kafka.clients.consumer.ConsumerRecord
import scala.collection.JavaConverters._

case class AsyncStreamKafkaConsumerBuilder[K, V](
  asyncStreamConsumerConfig: AsyncStreamKafkaConsumerConfig[K, V] =
    AsyncStreamKafkaConsumerConfig[K, V]())
    extends FinagleKafkaConsumerBuilderMethods[K, V, AsyncStreamKafkaConsumerBuilder[K, V]] {
  override protected def fromFinagleConsumerConfig(config: FinagleKafkaConsumerConfig[K, V]): This =
    AsyncStreamKafkaConsumerBuilder(
      asyncStreamConsumerConfig.copy(finagleKafkaConsumerConfig = config)
    )

  override def config: FinagleKafkaConsumerConfig[K, V] =
    asyncStreamConsumerConfig.finagleKafkaConsumerConfig

  def topics(topics: Set[KafkaTopic]): This =
    AsyncStreamKafkaConsumerBuilder(asyncStreamConsumerConfig.copy(topics = topics))

  def subscribe(): ClosableAsyncStream[ConsumerRecord[K, V]] = {
    new ClosableAsyncStream[ConsumerRecord[K, V]] {

      private val consumer = build()

      consumer.subscribe(asyncStreamConsumerConfig.topics)

      def asyncStream(): AsyncStream[ConsumerRecord[K, V]] = {
        val futureConsumerRecords: Future[Seq[ConsumerRecord[K, V]]] = consumer
          .poll(asyncStreamConsumerConfig.finagleKafkaConsumerConfig.pollTimeout).map(
            _.iterator().asScala.toSeq
          )
        val asyncStreamOfSeqs: AsyncStream[Seq[ConsumerRecord[K, V]]] =
          AsyncStream.fromFuture(futureConsumerRecords)
        asyncStreamOfSeqs.flatMap(AsyncStream.fromSeq) ++ asyncStream()
      }

      override def close(deadline: Time): Future[Unit] = {
        consumer.close(deadline)
      }
    }
  }
}

case class AsyncStreamKafkaConsumerConfig[K, V](
  finagleKafkaConsumerConfig: FinagleKafkaConsumerConfig[K, V] = FinagleKafkaConsumerConfig[K, V](),
  topics: Set[KafkaTopic] = Set.empty)
