package com.twitter.finatra.conversions

import scala.util.matching.Regex

object pattern {

  implicit class RichRegex(underlying: Regex) {
    def matches(s: String) = {
      underlying.pattern.matcher(s).matches
    }
  }

}