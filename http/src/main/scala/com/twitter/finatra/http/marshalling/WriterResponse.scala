package com.twitter.finatra.http.marshalling

case class WriterResponse(
  contentType: String,
  body: Any,
  headers: Map[String, String] = Map.empty
)
