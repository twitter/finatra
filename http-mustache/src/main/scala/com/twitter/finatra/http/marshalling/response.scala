package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.{Fields, MediaType}
import com.twitter.finatra.http.response.EnrichedResponse

object response {

  /** Adds methods to [[EnrichedResponse]] which handle mustache template rendering */
  implicit class RichEnrichedResponse(val self: EnrichedResponse) extends AnyVal {

    /**
     * Return a response with the given Mustache template and object data rendered and written as
     * the response body.
     *
     * @param template the template name to render.
     * @param obj the data to render.
     * @return an [[com.twitter.finatra.http.response.EnrichedResponse]] with the given Mustache template object written as the
     *         response body.
     */
    def view(template: String, obj: Any): EnrichedResponse =
      self.body(MustacheBodyComponent(obj, template, getContentType.getOrElse(MediaType.Html)))

    /**
     * Return a response with the given Mustache object data rendered and written as
     * the response body.
     *
     * @param obj object data to render.
     * @return an [[com.twitter.finatra.http.response.EnrichedResponse]] with the given Mustache template object written as the
     *         response body.
     */
    def view(obj: Any): EnrichedResponse = view("", obj)

    private[this] def getContentType = self.underlying.headerMap.get(Fields.ContentType)
  }
}
