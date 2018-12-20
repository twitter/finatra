package com.twitter.finatra.kafka.modules

import com.twitter.app.Flag
import com.twitter.inject.TwitterModule

/**
 * Use this module when your app connects to a kafka cluster.  Your app should use this flag
 * to indicate which kafka cluster your app should talk to.
 */
object KafkaBootstrapModule extends TwitterModule {

  val kafkaBootstrapServers: Flag[String] =
    flag[String](
      "kafka.bootstrap.servers",
      "Destination of kafka bootstrap servers.  Can be a wily path, or a host:port"
    )
}
