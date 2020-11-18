package com.twitter.inject.conversions

import com.twitter.conversions.StringOps

object string {

  implicit class RichString(val self: String) extends AnyVal {
    @deprecated("Use com.twitter.conversions.StringOps#toOption instead", "2020-11-16")
    def toOption: Option[String] = StringOps.toOption(self)

    @deprecated("Use com.twitter.conversions.StringOps#getOrElse instead", "2020-11-16")
    def getOrElse(default: => String): String = StringOps.getOrElse(self, default)

    def ellipse(len: Int): String = {
      if (self.length <= len) {
        self
      } else {
        self.take(len) + "..."
      }
    }

  }
}
