package com.twitter.finatra.kafkastreams.config

import com.twitter.conversions.DurationOps._
import com.twitter.conversions.StorageUnitOps._
import java.util
import org.apache.kafka.common.config.TopicConfig.{
  CLEANUP_POLICY_COMPACT,
  CLEANUP_POLICY_CONFIG,
  DELETE_RETENTION_MS_CONFIG,
  SEGMENT_BYTES_CONFIG
}
import scala.collection.JavaConverters._
import scala.collection.mutable

object DefaultTopicConfig {

  /**
   * Default changelog topic configs generally suitable for non-windowed use cases using FinatraTransformer.
   * We explicitly do not enable cleanup-policy: compact,delete
   * because we'd rather rely on FinatraTransformer PersistentTimers to handle expiration/deletes
   * (which gives us more control over when and how expiration's can occur).
   */
  def FinatraChangelogConfig: util.Map[String, String] = mutable
    .Map(
      CLEANUP_POLICY_CONFIG -> CLEANUP_POLICY_COMPACT,
      SEGMENT_BYTES_CONFIG -> 100.megabytes.inBytes.toString,
      DELETE_RETENTION_MS_CONFIG -> 5.minutes.inMillis.toString //configure delete retention such that standby replicas have 5 minutes to read deletes
    ).asJava
}
