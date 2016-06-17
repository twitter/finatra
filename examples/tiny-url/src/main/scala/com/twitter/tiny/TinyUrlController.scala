package com.twitter.tiny

import java.net.URL
import javax.inject.Inject

import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import com.twitter.tiny.domain.http.{PostUrlRequest, PostUrlResponse, TinyUrlRedirect}
import com.twitter.tiny.services.UrlShortenerService

class TinyUrlController @Inject()(
  @Flag("secure") secure: Boolean,
  urlShortenerService: UrlShortenerService,
  response: ResponseBuilder)
  extends Controller
  with Logging {

  post("/url") { request: PostUrlRequest =>
    val url = new URL(request.url)
    val path = urlShortenerService.create(url)
    // return the url in the location header
    val protocol = if (secure) "https" else "http"
    val base = request.request.host getOrElse "localhost"
    response.created(PostUrlResponse(s"$protocol://$base/$path"))
  }

  get("/:id") { request: TinyUrlRedirect =>
    urlShortenerService.get(request.id) match {
      case Some(url) =>
        info(s"Redirecting to resolved URL for id: ${request.id} -> $url")
        response.movedPermanently.location(url)
      case _ => response.notFound
    }
  }
}
