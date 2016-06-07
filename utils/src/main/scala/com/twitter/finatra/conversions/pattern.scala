package com.twitter.finatra.conversions

import scala.util.matching.Regex

object pattern {

  implicit class RichRegex(val self: Regex) extends AnyVal {
    def matches(s: String) = {
      self.pattern.matcher(s).matches
    }
  }

}