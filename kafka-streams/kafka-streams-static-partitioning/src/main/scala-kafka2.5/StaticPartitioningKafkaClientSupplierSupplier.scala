package com.twitter.finatra.kafkastreams.partitioning.internal

import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier

class StaticPartitioningKafkaClientSupplierSupplier(
  numApplicationInstances: Int)
    extends DefaultKafkaClientSupplier {}
