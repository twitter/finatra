package com.twitter.inject.conversions

import com.twitter.util.{Duration => TwitterDuration}
import org.joda.time.Duration

object duration {

  implicit class RichDuration(val self: Duration) extends AnyVal {
    def toTwitterDuration: TwitterDuration = {
      TwitterDuration.fromMilliseconds(
        self.getMillis)
    }
  }

  implicit class RichTwitterDuration(val self: TwitterDuration) extends AnyVal {
    def toJodaDuration: Duration = {
      new Duration(self.inMillis)
    }
  }

}