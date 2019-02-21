package com.twitter.finatra.kafkastreams.flushing

import org.apache.kafka.streams.kstream.Transformer

trait FlushingTransformer[K, V, K1, V1] extends Transformer[K, V, (K1, V1)] with Flushing
