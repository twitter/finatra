package com.twitter.finatra.kafkastreams.integration.async_transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.test.KafkaStreamsFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.util.Try
import org.apache.kafka.common.serialization.Serdes

class WordLookupThreadpoolServerFeatureTestBase extends KafkaStreamsFeatureTest {

  private val numberOfWords = 30

  override val server = new EmbeddedTwitterServer(
    new WordLookupThreadpoolServer(expectedOnFutureSuccessCount = numberOfWords),
    flags = kafkaStreamsFlags ++ Map(
      "kafka.application.id" -> "word-lookup-threadpool",
      "kafka.commit.interval" -> "1.second"
    ),
    disableTestLogging = true
  )

  private val textLinesTopic = kafkaTopic(
    ScalaSerdes.Long,
    Serdes.String,
    "TextLinesTopic",
    logPublish = true
  )

  private val wordsWithCountsTopic = kafkaTopic(Serdes.String, Serdes.Long, "WordToWordLength")

  if (!sys.props.contains("SKIP_FLAKY_TRAVIS")) {
    // This test was added to address the race condition where some of the callbacks in
    // AsyncFlushing.addFuture only complete after onFlush completes when the AsyncTransformer is
    // used with a threadpool.
    test("all onFutureSuccess calls complete before onFlush completes") {

      val word = "hello"

      val words = List.fill(numberOfWords)(word)
      val wordsToLengths = words.map(word => (word, word.length))

      server.start()

      textLinesTopic.publish(1L -> words.mkString(" "))
      wordsWithCountsTopic.consumeMessages(
        numMessages = numberOfWords,
        90.seconds) should contain theSameElementsAs wordsToLengths
      assertOutputTopicEmpty()

      server.close()

      await(server.mainResult)
      // Make sure assertion in onFlush did not throw an exception
      server.injectableServer
        .asInstanceOf[KafkaStreamsTwitterServer].uncaughtException shouldBe null
    }
  }

  private def assertOutputTopicEmpty(): Unit = {
    val otherResults =
      Try(wordsWithCountsTopic.consumeMessages(numMessages = 1, 2.seconds)).getOrElse(Seq.empty)
    otherResults should equal(Seq.empty)
  }
}
