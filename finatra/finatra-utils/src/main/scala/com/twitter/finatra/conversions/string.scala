package com.twitter.finatra.conversions

import org.apache.commons.lang.StringUtils

object string {
  
  implicit class RichString(wrapped: String) {
    def toOption = {
      if (wrapped == null || wrapped.isEmpty)
        None
      else
        Some(wrapped)
    }

    def ellipse(len: Int) = {
      StringUtils.abbreviate(wrapped, len + 3) // adding 3 for the ellipses :-/
    }
  }
}
