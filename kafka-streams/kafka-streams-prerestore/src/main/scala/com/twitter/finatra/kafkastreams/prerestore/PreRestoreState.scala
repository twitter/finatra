package com.twitter.finatra.kafkastreams.prerestore

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.annotations.Experimental
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.internal.utils.ReflectionUtils
import com.twitter.finatra.kafkastreams.partitioning.StaticPartitioning
import com.twitter.finatra.kafkastreams.internal.utils.CompatibleUtils

import java.util.Properties
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.Metric
import org.apache.kafka.common.utils.Utils
import org.apache.kafka.streams.processor.internals.StreamThread
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.joda.time.DateTimeUtils
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

@Experimental
trait PreRestoreState extends KafkaStreamsTwitterServer with StaticPartitioning {

  private val preRestoreState = flag("kafka.prerestore.state", true, "Pre-Restore state")
  private val preRestoreDurationInitialDelay =
    flag("kafka.prerestore.duration", 2.minutes, "Pre-Restore min delay")

  /* Protected */

  final override protected[finatra] def createAndStartKafkaStreams(): Unit = {
    if (preRestoreState()) {
      val copiedProperties = properties.clone().asInstanceOf[Properties]
      val preRestoreProperties = configurePreRestoreProperties(copiedProperties)
      val preRestoreKafkaStreams =
        new KafkaStreams(topology, preRestoreProperties, kafkaStreamsClientSupplier)
      setExceptionHandler(preRestoreKafkaStreams)
      preRestoreKafkaStreams.start()
      startWaitForPreRestoredThread(preRestoreKafkaStreams)
    } else {
      super.createAndStartKafkaStreams()
    }
  }

  /* Private */

  private def startWaitForPreRestoredThread(preRestoreKafkaStreams: KafkaStreams): Unit = {
    new Thread("wait-for-pre-restoring-server-thread") {
      override def run(): Unit = {
        try {
          waitForPreRestoreFinished(preRestoreKafkaStreams)

          info(s"Closing pre-restoring server")
          preRestoreKafkaStreams.close(1, TimeUnit.MINUTES)
          info(s"Pre-restore complete.")

          //Reset the thread id and start Kafka Streams as if we weren't using pre-restore mode
          CompatibleUtils.resetStreamThreadId()
          PreRestoreState.super.createAndStartKafkaStreams()
        } catch {
          case NonFatal(e) =>
            error("PreRestore error", e)
            close(defaultCloseGracePeriod)
        }
      }
    }.start()
  }

  // Note: 10000 is somewhat arbitrary. The goal is to get close to the head of the changelog, before exiting pre-restore mode and taking active ownership of the pre-restoring tasks
  private def waitForPreRestoreFinished(preRestoreKafkaStreams: KafkaStreams): Unit = {
    info(
      s"Waiting for Total Restore Lag to be less than 1000 after an initial wait period of ${preRestoreDurationInitialDelay()}"
    )
    val minTimeToFinish = DateTimeUtils
      .currentTimeMillis() + preRestoreDurationInitialDelay().inMillis
    var totalRestoreLag = Double.MaxValue
    while (totalRestoreLag >= 10000 || DateTimeUtils.currentTimeMillis() < minTimeToFinish) {
      totalRestoreLag = findTotalRestoreLag(preRestoreKafkaStreams)
      Thread.sleep(1000)
    }
  }

  private def findTotalRestoreLag(preRestoreKafkaStreams: KafkaStreams): Double = {
    val lagMetrics = findRestoreConsumerLagMetrics(preRestoreKafkaStreams)
    val totalRestoreLag = lagMetrics.map(_.metricValue.asInstanceOf[Double]).sum
    info(s"Total Restore Lag: $totalRestoreLag")
    totalRestoreLag
  }

  private def configurePreRestoreProperties(properties: Properties) = {
    val applicationServerConfigHost = Utils.getHost(applicationServerConfig())
    properties.put(
      StreamsConfig.APPLICATION_SERVER_CONFIG,
      s"$applicationServerConfigHost:${StaticPartitioning.PreRestoreSignalingPort}"
    )

    // During prerestore we set poll_ms to 0 to prevent activeTask.polling from slowing down the standby tasks
    // See https://github.com/apache/kafka/blob/b532ee218e01baccc0ff8c4b1df586577637de50/streams/src/main/java/org/apache/kafka/streams/processor/internals/StreamThread.java#L832
    properties.put(StreamsConfig.POLL_MS_CONFIG, "0")

    properties
  }

  private def findRestoreConsumerLagMetrics(kafkaStreams: KafkaStreams): Seq[Metric] = {
    for {
      thread <- getThreads(kafkaStreams).toSeq
      restoreConsumer = getRestoreConsumer(thread)
      (name, recordsLag) <- findConsumerLagMetric(restoreConsumer)
    } yield {
      recordsLag
    }
  }

  private def findConsumerLagMetric(restoreConsumer: Consumer[Array[Byte], Array[Byte]]) = {
    restoreConsumer.metrics().asScala.find {
      case (metricName, metric) => metricName.name() == "records-lag"
    }
  }

  private def getThreads(kafkaStreams: KafkaStreams) = {
    ReflectionUtils.getFinalField[Array[StreamThread]](anyRef = kafkaStreams, fieldName = "threads")
  }

  private def getRestoreConsumer(thread: StreamThread) = {
    ReflectionUtils.getFinalField[Consumer[Array[Byte], Array[Byte]]](thread, "restoreConsumer")
  }
}
