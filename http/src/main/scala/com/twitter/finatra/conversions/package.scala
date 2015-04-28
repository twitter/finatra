package com.twitter.finatra

package object conversions {
  @deprecated("Use com.twitter.finatra.http.conversions.futureHttp", "")
  val futureHttp = com.twitter.finatra.http.conversions.futureHttp

  @deprecated("Use com.twitter.finatra.http.conversions.optionHttp", "")
  val optionHttp = com.twitter.finatra.http.conversions.optionHttp
}
