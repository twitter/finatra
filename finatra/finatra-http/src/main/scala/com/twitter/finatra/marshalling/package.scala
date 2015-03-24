package com.twitter.finatra

package object marshalling {
  @deprecated("MessageBodyManager is an internal class. Use the HttpMockResponses trait to gain access to a testResponseBuilder", "")
  type MessageBodyManager = com.twitter.finatra.internal.marshalling.MessageBodyManager
}
