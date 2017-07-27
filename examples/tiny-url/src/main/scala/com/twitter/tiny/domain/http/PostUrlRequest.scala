package com.twitter.tiny.domain.http

import com.twitter.finagle.http.Request

case class PostUrlRequest(request: Request, url: String)
