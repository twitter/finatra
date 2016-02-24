package com.twitter.finatra.http.marshalling.mustache

import com.twitter.finatra.http.marshalling.MessageBodyComponent
import javax.inject.Inject

case class MustacheBodyComponent @Inject()(
  data: Any,
  template: String)
  extends MessageBodyComponent
