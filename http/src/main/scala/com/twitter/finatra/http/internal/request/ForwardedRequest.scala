package com.twitter.finatra.http.internal.request

import com.twitter.finagle.http.{Request, RequestProxy}

private[http] class ForwardedRequest(
  wrapped: Request,
  path: String)
  extends RequestProxy {

  def request: Request = wrapped

  /**
   * Returns the URI of this request.
   */
  override def uri: String = path
}
