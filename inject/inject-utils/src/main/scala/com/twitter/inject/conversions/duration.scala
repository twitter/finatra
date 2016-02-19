package com.twitter.inject.conversions

import com.twitter.util.{Duration => TwitterDuration}
import org.joda.time.Duration

object duration {

  implicit class RichDuration(duration: Duration) {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        duration.getMillis)
    }
  }

  implicit class RichTwitterDuration(duration: TwitterDuration) {
    def toJodaDuration: Duration = {
      new Duration(duration.inMillis)
    }
  }

}