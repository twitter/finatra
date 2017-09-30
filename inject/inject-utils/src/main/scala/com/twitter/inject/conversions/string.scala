package com.twitter.inject.conversions

import org.apache.commons.lang.StringUtils

object string {

  implicit class RichString(val self: String) extends AnyVal {
    def toOption = {
      if (self == null || self.isEmpty)
        None
      else
        Some(self)
    }

    def getOrElse(default : => String) = {
      if (self == null || self.isEmpty)
        default
      else
        self
    }

    def ellipse(len: Int) = {
      StringUtils.abbreviate(self, len + 3) // adding 3 for the ellipses :-/
    }
  }
}
