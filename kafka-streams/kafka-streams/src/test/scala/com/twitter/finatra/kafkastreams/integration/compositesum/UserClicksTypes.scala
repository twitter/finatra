package com.twitter.finatra.kafkastreams.integration.compositesum

import com.twitter.finatra.kafka.serde.ScalaSerdes
import org.apache.kafka.common.serialization.Serde

object UserClicksTypes {
  type UserId = Int
  val UserIdSerde: Serde[Int] = ScalaSerdes.Int

  type NumClicks = Int
  val NumClicksSerde: Serde[Int] = ScalaSerdes.Int

  type ClickType = Int
  val ClickTypeSerde: Serde[Int] = ScalaSerdes.Int
}
