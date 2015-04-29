package com.twitter.finatra

package object marshalling {
  @deprecated("MessageBodyManager is an internal class. Use the HttpMockResponses trait to gain access to a testResponseBuilder", "")
  type MessageBodyManager = com.twitter.finatra.http.internal.marshalling.MessageBodyManager

  @deprecated("Use com.twitter.finatra.http.marshalling.DefaultMessageBodyReader", "")
  type DefaultMessageBodyReader = com.twitter.finatra.http.marshalling.DefaultMessageBodyReader

  @deprecated("Use com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter", "")
  type DefaultMessageBodyWriter = com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter

  @deprecated("Use com.twitter.finatra.http.marshalling.MessageBodyComponent", "")
  type MessageBodyComponent = com.twitter.finatra.http.marshalling.MessageBodyComponent

  @deprecated("Use com.twitter.finatra.http.marshalling.MessageBodyReader", "")
  type MessageBodyReader[T] = com.twitter.finatra.http.marshalling.MessageBodyReader[T]

  @deprecated("Use com.twitter.finatra.http.marshalling.MessageBodyWriter", "")
  type MessageBodyWriter[T] = com.twitter.finatra.http.marshalling.MessageBodyWriter[T]

  @deprecated("Use com.twitter.finatra.http.marshalling.WriterResponse", "")
  type WriterResponse = com.twitter.finatra.http.marshalling.WriterResponse

  @deprecated("Use com.twitter.finatra.http.marshalling.WriterResponse", "")
  val WriterResponse = com.twitter.finatra.http.marshalling.WriterResponse
}
