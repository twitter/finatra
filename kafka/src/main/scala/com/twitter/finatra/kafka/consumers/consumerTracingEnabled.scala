package com.twitter.finatra.kafka.consumers

import com.twitter.app.GlobalFlag

/**
 * [[GlobalFlag]] to enable/disable tracing for Kafka consumers. Disabled by default.
 */
object consumerTracingEnabled
    extends GlobalFlag[Boolean](false, "Enable/disable tracing for Kafka consumers")
