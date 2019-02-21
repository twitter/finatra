package com.twitter.finatra.kafka.consumers

import com.twitter.app.Flaggable
import com.twitter.finatra.kafka.domain.SeekStrategy
import org.apache.kafka.clients.consumer.OffsetResetStrategy

/**
 * Contains implicit Flaggable implementations for various kafka configuration types.
 */
object Flaggables {

  /**
   * Allows you to create a flag which will convert the flag's input String into a
   * [[com.twitter.finatra.kafka.domain.SeekStrategy]]
   *
   * {{{
   * import com.twitter.fanatra.kafka.consumers.Flaggables.seekStrategyFlaggable
   *
   * private val seekStrategyFlag = flag[SeekStrategy](
   *   "seek.strategy.flag",
   *   SeekStrategy.RESUME,
   *   "This is the seek strategy flag"
   * )
   * }}}
   */
  implicit val seekStrategyFlaggable: Flaggable[SeekStrategy] = new Flaggable[SeekStrategy] {
    override def parse(s: String): SeekStrategy = s match {
      case "beginning" => SeekStrategy.BEGINNING
      case "end" => SeekStrategy.END
      case "resume" => SeekStrategy.RESUME
      case "rewind" => SeekStrategy.REWIND
      case _ => throw new IllegalArgumentException(s"$s is not a valid seek strategy.")
    }
  }

  /**
   * Allows you to create a flag which will convert the flag's input String into a
   * [[org.apache.kafka.clients.consumer.OffsetResetStrategy]]
   *
   * {{{
   * import org.apache.kafka.clients.consumer.OffsetResetStrategy
   *
   * private val offsetResetStrategyFlag = flag[OffsetResetStrategy](
   *   "offset.reset.strategy.flag",
   *   OffsetResetStrategy.LATEST,
   *   "This is the offset reset strategy flag"
   * )
   * }}}
   */
  implicit val offsetResetStrategyFlaggable: Flaggable[OffsetResetStrategy] =
    new Flaggable[OffsetResetStrategy] {
      override def parse(s: String): OffsetResetStrategy = s match {
        case "latest" => OffsetResetStrategy.LATEST
        case "earliest" => OffsetResetStrategy.EARLIEST
        case "none" => OffsetResetStrategy.NONE
        case _ => throw new IllegalArgumentException(s"$s is not a valid offset reset strategy")
      }
    }
}
