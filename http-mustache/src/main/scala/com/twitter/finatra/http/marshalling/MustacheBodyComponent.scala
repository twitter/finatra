package com.twitter.finatra.http.marshalling

import jakarta.inject.Inject

case class MustacheBodyComponent @Inject() (
  data: Any,
  templateName: String,
  contentType: String)
    extends MessageBodyComponent
