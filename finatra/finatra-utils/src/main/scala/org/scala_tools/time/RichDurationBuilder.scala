package org.scala_tools.time

import com.twitter.util.Duration

trait RichDurationBuilder {
  implicit def durationBuilderToRichDurationBuilder(durationBuilder: DurationBuilder) = new {
    def toTwitterDuration: Duration = {
      Duration.fromMilliseconds(
        durationBuilder.toDuration.getMillis)
    }
  }
}
