package com.twitter.finatra.kafka.test

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.interceptors.PublishTimeProducerInterceptor
import com.twitter.finatra.kafka.test.utils.PollUtils
import com.twitter.finatra.kafka.test.utils.ThreadUtils
import com.twitter.util.jackson.JsonDiff
import com.twitter.util.Duration
import com.twitter.util.Time
import com.twitter.util.TimeoutException
import com.twitter.util.logging.Logging
import java.util.concurrent.LinkedBlockingQueue
import java.util.Collections
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.integration.utils.EmbeddedKafkaCluster
import org.scalatest.matchers.should.Matchers
import org.slf4j.event.Level
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * Used to read/write from Kafka topics created on local brokers during testing.
 *
 * @param topic the topic to write to
 * @param keySerde the serde for the key
 * @param valSerde the serde for the value
 * @param _kafkaCluster the kafka cluster to use to produce/consume from
 * @param partitions the number of partitions for this topic
 * @param replication tge replication factor for this topic
 * @param autoConsume whether or not to automatically consume messages off this topic(useful for logging)
 * @param autoCreate whether or not to automatically create this topic on the brokers
 * @param logPublishes whether or not to publish logs
 * @param allowPublishes whether or not this topic allows publishes
 * @tparam K the type of the key
 * @tparam V the type of the value
 */
case class KafkaTopic[K, V](
  topic: String,
  keySerde: Serde[K],
  valSerde: Serde[V],
  _kafkaCluster: () => EmbeddedKafkaCluster,
  partitions: Int = 1,
  replication: Int = 1,
  autoConsume: Boolean = true, //TODO: Rename autoConsume
  autoCreate: Boolean = true,
  logPublishes: Boolean = true,
  allowPublishes: Boolean = true)
    extends Logging
    with Matchers {

  private val defaultConsumeTimeout = 60.seconds
  private lazy val kafkaCluster = _kafkaCluster()
  private lazy val producer = new KafkaProducer[Array[Byte], Array[Byte]](producerConfig)
  private[twitter] lazy val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](consumerConfig)
  private val consumedMessages = new LinkedBlockingQueue[ConsumerRecord[Array[Byte], Array[Byte]]]()
  @volatile private var running = false
  @volatile private var failure: Throwable = _

  private val keyDeserializer = keySerde.deserializer()
  private val valueDeserializer = valSerde.deserializer()

  /* Public */

  def init(): Unit = {
    running = true
    if (autoConsume) {
      ThreadUtils.fork {
        try {
          consumer.subscribe(Collections.singletonList(topic))

          while (running) {
            val consumerRecords = consumer.poll(java.time.Duration.ofMillis(Long.MaxValue))
            for (record <- consumerRecords.iterator().asScala) {
              val (key, value) = deserializeKeyValue(record)
              debug(
                f"@${dateTimeStr(record)}%-24s ${topic + "_" + record.partition}%-80s$key%-50s -> $value"
              )

              consumedMessages.put(record)
            }
          }

          consumer.close()
        } catch {
          case NonFatal(e) =>
            running = false
            failure = e
            error(s"Error reading KafkaTopic $topic", e)
        }
      }
    }
  }

  private def dateTimeStr(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]]): String = {
    val timestamp = consumerRecord.timestamp()
    dateTimeStr(timestamp)
  }

  private def dateTimeStr(timestamp: Long) = {
    if (timestamp == Long.MaxValue) {
      "MaxWatermark"
    } else {
      Time.fromMilliseconds(timestamp).toString
    }
  }

  def close(): Unit = {
    running = false
    producer.close()
    assert(failure == null, s"There was an error consuming from KafkaTopic $topic " + failure)
  }

  def publish(
    keyValue: (K, V),
    timestamp: Long = System.currentTimeMillis(),
    headers: Iterable[Header] = Seq.empty[Header]
  ): Unit = {
    assert(allowPublishes)
    val (key, value) = keyValue
    val producerRecord = new ProducerRecord(
      topic,
      null,
      timestamp,
      keySerde.serializer().serialize(topic, key),
      valSerde.serializer().serialize(topic, value),
      headers.asJava
    )

    val sendResult = producer.send(producerRecord).get()
    if (logPublishes) {
      info(
        f"@${dateTimeStr(timestamp)}%-24s ${topic + "_" + sendResult.partition}%-80s$key%-50s -> $value"
      )
    }
  }

  def publishUnkeyedValue(value: V, timestamp: Long = System.currentTimeMillis()): Unit = {
    publish(keyValue = null.asInstanceOf[K] -> value, timestamp = timestamp)
  }

  def consumeValue(): V = consumeValue(defaultConsumeTimeout)

  def consumeValue(timeout: Duration = defaultConsumeTimeout): V = consumeValues(1, timeout).head

  def consumeValues(numValues: Int, timeout: Duration = defaultConsumeTimeout): Seq[V] =
    consumeMessages(numValues, timeout).map(kv => kv._2)

  def consumeMessage(): (K, V) = consumeMessages(1, defaultConsumeTimeout).head

  def consumeRecord(): ConsumerRecord[K, V] = {
    val record = consumeRecords(1).head

    val (k, v) = deserializeKeyValue(record)

    new ConsumerRecord(
      record.topic(),
      record.partition(),
      record.offset(),
      record.timestamp(),
      record.timestampType(),
      record.checksum(),
      record.serializedKeySize(),
      record.serializedValueSize(),
      k,
      v,
      record.headers()
    )
  }

  def consumeMessage(timeout: Duration = defaultConsumeTimeout): (K, V) = {
    consumeMessages(1, timeout).head
  }

  def consumeMessages(numMessages: Int, timeout: Duration = defaultConsumeTimeout): Seq[(K, V)] = {
    assert(failure == null, s"There was an error consuming from KafkaTopic $topic " + failure)
    assert(autoConsume)
    if (!running) {
      init()
    }

    val resultBuilder = Seq.newBuilder[(K, V)]
    resultBuilder.sizeHint(numMessages)
    val endTime = System.currentTimeMillis() + timeout.inMillis

    var messagesRemaining = numMessages
    while (messagesRemaining > 0) {
      if (System.currentTimeMillis() > endTime) {
        throw new TimeoutException(s"Timeout waiting to consume $numMessages messages")
      }

      val pollResult = consumedMessages.poll()
      if (pollResult != null) {
        messagesRemaining -= 1
        trace(s"Poll result w/ messages remaining $messagesRemaining: " + pollResult)
        val (key, value) = deserializeKeyValue(pollResult)

        resultBuilder += ((key, value))
      }

      if (messagesRemaining > 0) {
        Thread.sleep(5)
      }
    }

    resultBuilder.result()
  }

  def consumeRecords(
    numMessages: Int,
    timeout: Duration = defaultConsumeTimeout
  ): Seq[ConsumerRecord[Array[Byte], Array[Byte]]] = {
    assert(failure == null, s"There was an error consuming from KafkaTopic $topic " + failure)
    assert(autoConsume)
    if (!running) {
      init()
    }

    val resultBuilder = Seq.newBuilder[ConsumerRecord[Array[Byte], Array[Byte]]]
    resultBuilder.sizeHint(numMessages)
    val endTime = System.currentTimeMillis() + timeout.inMillis

    var messagesRemaining = numMessages
    while (messagesRemaining > 0) {
      if (System.currentTimeMillis() > endTime) {
        throw new TimeoutException(s"Timeout waiting to consume $numMessages messages")
      }

      val pollResult = consumedMessages.poll()
      if (pollResult != null) {
        messagesRemaining -= 1
        trace(s"Poll result w/ messages remaining $messagesRemaining: " + pollResult)

        resultBuilder += pollResult
      }

      if (messagesRemaining > 0) {
        Thread.sleep(5)
      }
    }

    resultBuilder.result()
  }

  /**
   * Note: This method may consume more messages than the expected number of keys
   */
  def consumeAsManyMessagesUntil(
    timeout: Duration = defaultConsumeTimeout,
    exhaustedTimeoutMessage: => String = "",
    exhaustedTriesMessage: => String = ""
  )(
    until: ((K, V)) => Boolean
  ): (K, V) = {
    try {
      PollUtils.poll(
        func = consumeMessage(
          Duration.fromMinutes(999 * 60)
        ), //Note: Set set a high duration here so that we rely on PollUtils to enforce the duration
        exhaustedTriesMessage = (_: (K, V)) => exhaustedTriesMessage,
        exhaustedTimeoutMessage = exhaustedTimeoutMessage,
        timeout = timeout,
        sleepDuration = 0.millis
      )(until = until)
    } catch {
      case e: com.twitter.util.TimeoutException =>
        warn(exhaustedTimeoutMessage)
        throw e
    }
  }

  /**
   * Note: This method may consume more messages than the expected number of keys
   */
  //TODO: DRY
  def consumeAsManyMessagesUntilMap(
    expected: Map[K, V],
    timeout: Duration = defaultConsumeTimeout,
    logLevel: Level = Level.TRACE
  ): (K, V) = {
    val unSeenKeys = expected.keySet.toBuffer
    consumeAsManyMessagesUntil(
      timeout,
      exhaustedTimeoutMessage = s"UnSeenKeys: $unSeenKeys",
      exhaustedTriesMessage = s"UnSeenKeys: $unSeenKeys"
    ) {
      case (key, value) =>
        if (expected.get(key).contains(value)) {
          unSeenKeys -= key
          log(logLevel, s"Match $key $value $expected UnseenKeys $unSeenKeys")
        } else {
          log(logLevel, s"NoMatch $key $value $expected UnseenKeys $unSeenKeys")
        }
        unSeenKeys.isEmpty
    }
  }

  def consumeExpectedMap(expected: Map[K, V], timeout: Duration = defaultConsumeTimeout): Unit = {
    val receivedMap = consumeMessages(expected.size, timeout).toMap
    if (receivedMap != expected) {
      JsonDiff.assertDiff(expected, receivedMap)
    }
  }

  def toPrettyString: String = {
    s"$topic\tPartitions: $partitions Replication: $replication"
  }

  def clearConsumedMessages(): Unit = consumedMessages.clear()

  def numConsumedMessages: Int = consumedMessages.size

  /* Private */

  private lazy val producerConfig = {
    val config = new Properties
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaCluster.bootstrapServers)
    config.put(ProducerConfig.ACKS_CONFIG, "all")
    config.put(ProducerConfig.RETRIES_CONFIG, Integer.valueOf(0))
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer])
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer])
    config.put(
      ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
      classOf[PublishTimeProducerInterceptor].getName
    )
    config
  }

  private lazy val consumerConfig = {
    val consumerConfig = new Properties
    consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaCluster.bootstrapServers())
    consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-tester-consumer")
    consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    consumerConfig.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "5000")
    consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer])
    consumerConfig.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      classOf[ByteArrayDeserializer]
    )
    consumerConfig
  }

  private def log(level: Level, msg: String): Unit = {
    level match {
      case Level.ERROR => error(msg)
      case Level.WARN => warn(msg)
      case Level.INFO => info(msg)
      case Level.DEBUG => debug(msg)
      case Level.TRACE => trace(msg)
    }
  }

  private def deserializeKeyValue(record: ConsumerRecord[Array[Byte], Array[Byte]]): (K, V) = {
    val key = keyDeserializer.deserialize(topic, record.key())
    val recordValue = record.value()
    val value: V = if (recordValue == null) {
      null.asInstanceOf[V]
    } else {
      valueDeserializer.deserialize(topic, record.value())
    }

    (key, value)
  }
}
