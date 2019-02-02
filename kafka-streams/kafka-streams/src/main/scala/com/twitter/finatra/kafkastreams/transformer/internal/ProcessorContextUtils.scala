package com.twitter.finatra.kafkastreams.transformer.internal

import com.twitter.finatra.kafkastreams.internal.utils.ReflectionUtils
import java.lang.reflect.Field
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.internals.{
  AbstractProcessorContext,
  ProcessorRecordContext
}

object ProcessorContextUtils {

  private val processorRecordContextTimestampField: Field =
    ReflectionUtils.getFinalField(classOf[ProcessorRecordContext], "timestamp")

  //Workaround until new KIP code lands from: https://github.com/apache/kafka/pull/4519/files#diff-7fba7b13f10a41d067e38316bf3f01b6
  //See also: KAFKA-6454
  def setTimestamp(processorContext: ProcessorContext, newTimestamp: Long): Unit = {
    val processorRecordContext = processorContext
      .asInstanceOf[AbstractProcessorContext].recordContext.asInstanceOf[ProcessorRecordContext]
    processorRecordContextTimestampField.setLong(processorRecordContext, newTimestamp)
  }
}
