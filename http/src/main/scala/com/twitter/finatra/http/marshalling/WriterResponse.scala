package com.twitter.finatra.http.marshalling

// TODO: replace guava MediaType
case class WriterResponse(
  contentType: String,
  body: Any,
  headers: Map[String, String] = Map.empty
)
