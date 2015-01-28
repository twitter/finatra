package org.scala_tools.time

import com.twitter.util.{Duration => TwitterDuration}

trait RichDurationBuilder {
  implicit class RichDurationBuilder(durationBuilder: DurationBuilder) {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        durationBuilder.toDuration.getMillis)
    }
  }
}