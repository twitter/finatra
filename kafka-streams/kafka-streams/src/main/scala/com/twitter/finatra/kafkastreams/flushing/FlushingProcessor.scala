package com.twitter.finatra.kafkastreams.flushing

import com.twitter.finatra.kafkastreams.internal.utils.ProcessorContextLogging
import com.twitter.finatra.kafkastreams.transformer.lifecycle.OnInit
import org.apache.kafka.streams.processor._

trait FlushingProcessor[K, V]
    extends AbstractProcessor[K, V]
    with OnInit
    with Flushing
    with ProcessorContextLogging {

  private var _context: ProcessorContext = _

  override def init(processorContext: ProcessorContext): Unit = {
    _context = processorContext
    onInit()
  }

  override def processorContext: ProcessorContext = _context

  final override def close(): Unit = {
    onClose()
  }
}
