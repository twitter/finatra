package com.twitter.finatra.streams.config

import com.twitter.conversions.StorageUnitOps._
import com.twitter.conversions.DurationOps._
import java.util
import org.apache.kafka.common.config.TopicConfig.{
  CLEANUP_POLICY_COMPACT,
  CLEANUP_POLICY_CONFIG,
  DELETE_RETENTION_MS_CONFIG,
  SEGMENT_BYTES_CONFIG
}
import scala.collection.JavaConverters._

object DefaultTopicConfig {

  /*
      Note: The following default configuration options can be used for non-windowed or windowed
      use cases using FinatraTransformer. We explicitly do not enable cleanup-policy: compact,delete
      because we'd rather rely on FinatraTransformer PersistentTimers to handle expiration (which
      gives us more control over when and how expiration can occur).

      The approach described above differs from the Kafka Streams Windowing DSL which expires windows
      using a "Segmented" WindowKeyValueStore combined with broker side retention using retention.ms
      and a cleanup policy of compact,delete.
   */
  val FinatraChangelogConfig: util.Map[String, String] = Map(
    CLEANUP_POLICY_CONFIG -> CLEANUP_POLICY_COMPACT,
    SEGMENT_BYTES_CONFIG -> 100.megabytes.inBytes.toString,
    //configure delete retention such that standby replicas have 5 minutes to read deletes
    DELETE_RETENTION_MS_CONFIG -> 5.minutes.inMillis.toString
  ).asJava
}
