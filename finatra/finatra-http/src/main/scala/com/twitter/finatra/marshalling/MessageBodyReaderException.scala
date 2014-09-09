package com.twitter.finatra.marshalling


class MessageBodyReaderException(key: MessageBodyKey)
  extends Exception("No MessageBodyReader found for: " + key)
