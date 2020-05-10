package com.twitter.finatra.kafka.consumers

import com.twitter.finatra.kafka.consumers.Flaggables.{
  offsetResetStrategyFlaggable,
  seekStrategyFlaggable
}
import com.twitter.finatra.kafka.domain.SeekStrategy
import com.twitter.inject.Test
import org.apache.kafka.clients.consumer.OffsetResetStrategy

class FlaggablesTest extends Test {

  test("Flaggables#seekStrategyFlaggable") {
    seekStrategyFlaggable.parse("beginning") should equal(SeekStrategy.BEGINNING)
    seekStrategyFlaggable.parse("resume") should equal(SeekStrategy.RESUME)
    seekStrategyFlaggable.parse("rewind") should equal(SeekStrategy.REWIND)
    seekStrategyFlaggable.parse("end") should equal(SeekStrategy.END)
    an[IllegalArgumentException] should be thrownBy seekStrategyFlaggable.parse("unknown")
  }

  test("Flaggables#offsetResetStrategyFlaggable ") {
    offsetResetStrategyFlaggable.parse("latest") should equal(OffsetResetStrategy.LATEST)
    offsetResetStrategyFlaggable.parse("earliest") should equal(OffsetResetStrategy.EARLIEST)
    offsetResetStrategyFlaggable.parse("none") should equal(OffsetResetStrategy.NONE)
    an[IllegalArgumentException] should be thrownBy offsetResetStrategyFlaggable.parse("unknown")
  }
}
