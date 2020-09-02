package com.twitter.finatra.kafka

import com.twitter.app.GlobalFlag

/**
 * [[GlobalFlag]] to enable/disable tracing for Kafka producer and consumer. Enabled by default.
 */
object tracingEnabled extends GlobalFlag[Boolean](true, "Enable/disable tracing for Kafka clients")
