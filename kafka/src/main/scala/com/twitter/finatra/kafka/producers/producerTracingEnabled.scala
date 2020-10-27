package com.twitter.finatra.kafka.producers

import com.twitter.app.GlobalFlag

/**
 * [[GlobalFlag]] to enable/disable tracing for Kafka producers. Enabled by default.
 */
object producerTracingEnabled
    extends GlobalFlag[Boolean](true, "Enable/disable tracing for Kafka producers")
