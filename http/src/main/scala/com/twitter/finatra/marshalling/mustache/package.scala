package com.twitter.finatra.marshalling

package object mustache {
  @deprecated("MustacheService is an internal class. Use the HttpMockResponses trait to gain access to a testResponseBuilder", "")
  type MustacheService = com.twitter.finatra.http.internal.marshalling.mustache.MustacheService
}
