package com.twitter.finatra.kafkastreams.transformer.aggregation

case class WindowedValue[V](windowResultType: WindowResultType, value: V)
