package com.twitter.inject.conversions

object string {

  implicit class RichString(val self: String) extends AnyVal {
    def ellipse(len: Int): String = {
      if (self.length <= len) {
        self
      } else {
        self.take(len) + "..."
      }
    }

  }
}
