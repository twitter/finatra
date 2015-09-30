package com.twitter.inject.thrift.conversions

import com.twitter.util.{Duration => TwitterDuration}
import org.joda.time.Duration

object duration extends DurationConversions

trait DurationConversions {

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