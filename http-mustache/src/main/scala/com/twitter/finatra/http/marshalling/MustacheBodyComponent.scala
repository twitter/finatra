package com.twitter.finatra.http.marshalling

import javax.inject.Inject

case class MustacheBodyComponent @Inject()(
  data: Any,
  templateName: String,
  contentType: String)
  extends MessageBodyComponent
