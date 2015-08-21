package com.twitter.finatra

package object response {
  @deprecated("Use com.twitter.finatra.http.response.ErrorsResponse", "")
  type ErrorsResponse = com.twitter.finatra.http.response.ErrorsResponse

  @deprecated("Use com.twitter.finatra.http.response.ErrorsResponse", "")
  val ErrorsResponse = com.twitter.finatra.http.response.ErrorsResponse

  @deprecated("Use com.twitter.finatra.http.response.ResponseBuilder", "")
  type ResponseBuilder = com.twitter.finatra.http.response.ResponseBuilder
}
