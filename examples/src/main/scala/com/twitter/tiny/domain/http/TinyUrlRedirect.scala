package com.twitter.tiny.domain.http

import com.twitter.finatra.request.RouteParam

case class TinyUrlRedirect(@RouteParam id: String)
