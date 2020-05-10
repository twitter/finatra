package com.twitter.finatra.kafkastreams.flushing

import com.twitter.finatra.kafkastreams.internal.utils.ProcessorContextLogging
import com.twitter.finatra.kafkastreams.transformer.lifecycle.{OnClose, OnFlush, OnInit}
import com.twitter.util.Duration
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.processor.{Cancellable, PunctuationType, Punctuator}

trait Flushing extends OnInit with OnClose with OnFlush with ProcessorContextLogging {

  @volatile private var commitPunctuatorCancellable: Cancellable = _

  protected def commitInterval: Duration

  //TODO: Create and use frameworkOnInit for framework use
  override def onInit(): Unit = {
    super.onInit()

    val streamsCommitIntervalMillis = processorContext
      .appConfigs().get(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG).asInstanceOf[java.lang.Long]
    assert(
      streamsCommitIntervalMillis == Duration.Top.inMillis,
      s"You're using an operator that requires 'Flushing' functionality (e.g. FlushingProcessor/Transformer or AsyncProcessor/Transformer). As such, your server must mixin FlushingAwareServer so that automatic Kafka Streams commit will be disabled."
    )

    if (commitInterval != Duration.Top) {
      info(s"Scheduling timer to call commit every $commitInterval")
      commitPunctuatorCancellable = processorContext
        .schedule(
          commitInterval.inMillis,
          PunctuationType.WALL_CLOCK_TIME,
          new Punctuator {
            override def punctuate(timestamp: Long): Unit = {
              onFlush()
              processorContext.commit()
            }
          })
    }
  }

  //TODO: Create and use frameworkOnClose
  override def onClose(): Unit = {
    super.onClose()
    if (commitPunctuatorCancellable != null) {
      commitPunctuatorCancellable.cancel()
      commitPunctuatorCancellable = null
    }
  }
}
