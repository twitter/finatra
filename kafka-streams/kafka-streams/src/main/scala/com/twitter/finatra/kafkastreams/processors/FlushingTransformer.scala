package com.twitter.finatra.kafkastreams.processors

import com.twitter.finatra.kafkastreams.processors.internal.Flushing
import org.apache.kafka.streams.kstream.Transformer

trait FlushingTransformer[K, V, K1, V1] extends Transformer[K, V, (K1, V1)] with Flushing
