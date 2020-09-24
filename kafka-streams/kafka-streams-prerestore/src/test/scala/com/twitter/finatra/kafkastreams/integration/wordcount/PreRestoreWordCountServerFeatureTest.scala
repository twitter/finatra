package com.twitter.finatra.kafkastreams.integration.wordcount

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.internal.utils.CompatibleUtils
import com.twitter.finatra.kafkastreams.test.KafkaStreamsMultiServerFeatureTest
import com.twitter.util.{Await, Duration}
import org.apache.kafka.common.serialization.Serdes
import org.scalatest.Ignore

@Ignore
class PreRestoreWordCountServerFeatureTest extends KafkaStreamsMultiServerFeatureTest {

  private def createServer(preRestore: Boolean): EmbeddedHttpServer = {
    new EmbeddedHttpServer(
      new PreRestoreWordCountRocksDbServer,
      flags = kafkaStreamsFlags ++ Map(
        "kafka.application.num.instances" -> "1",
        "kafka.prerestore.state" -> s"$preRestore",
        "kafka.prerestore.duration" -> "1000.milliseconds",
        "kafka.application.server" -> "0.foo.scosenza.service.smf1.twitter.com:12345",
        "kafka.application.id" -> "wordcount-prod"
      )
    )
  }

  override protected def kafkaCommitInterval: Duration = 1.second

  private val textLinesTopic =
    kafkaTopic(ScalaSerdes.Long, Serdes.String, "TextLinesTopic", logPublish = false)
  private val countsChangelogTopic = kafkaTopic(
    Serdes.String,
    Serdes.Long,
    "wordcount-prod-CountsStore-changelog",
    autoCreate = false
  )
  private val wordsWithCountsTopic = kafkaTopic(Serdes.String, Serdes.Long, "WordsWithCountsTopic")

  test("word count") {
    testInitialStartupWithoutPrerestore()
    testRestartWithoutPrerestore()
    testRestartWithoutPrerestore()
    testRestartWithPrerestore()
  }

  private def testInitialStartupWithoutPrerestore(): Unit = {
    val server = createServer(preRestore = false)
    //val countsStore = kafkaStateStore[String, Long]("CountsStore", server)
    server.start()
    val serverStats = server.inMemoryStats

    textLinesTopic.publish(1L -> "hello world hello")
    /*countsStore.queryKeyValueUntilValue("hello", 2L)
    countsStore.queryKeyValueUntilValue("world", 1L)*/

    textLinesTopic.publish(1L -> "world world")
    //countsStore.queryKeyValueUntilValue("world", 3L)
    serverStats.gauges.waitFor("kafka/thread1/restore_consumer/records_consumed_total", 0.0f)

    for (i <- 1 to 1000) {
      textLinesTopic.publish(1L -> s"foo$i")
    }

    server.close()
    Await.result(server.mainResult)
    server.clearStats()
    CompatibleUtils.resetStreamThreadId()
  }

  private def testRestartWithoutPrerestore(): Unit = {
    val server = createServer(preRestore = false)
    /*val countsStore = kafkaStateStore[String, Long]("CountsStore", server)
    server.start()
    val serverStats = InMemoryStatsUtil(server.injector)

    countsStore.queryKeyValueUntilValue("hello", 2L)
    countsStore.queryKeyValueUntilValue("world", 3L)
    server.printStats()
    //TODO byte consumed is >0 but records consumed is 0 :-/ serverStats.waitForGauge("kafka/thread1/restore_consumer/records_consumed_total", 2)*/

    server.close()
    Await.result(server.mainResult)
    server.clearStats()
    resetStreamThreadId()
  }

  private def testRestartWithPrerestore(): Unit = {
    val server = createServer(preRestore = true)
    /*val countsStore = kafkaStateStore[String, Long]("CountsStore", server)
    server.start()
    val serverStats = InMemoryStatsUtil(server.injector)

    countsStore.queryKeyValueUntilValue("hello", 2L)
    countsStore.queryKeyValueUntilValue("world", 3L)
    serverStats.waitForGauge("kafka/thread1/restore_consumer/records_consumed_total", 0)*/

    server.close()
    Await.result(server.mainResult)
    server.clearStats()
    resetStreamThreadId()
  }
}
