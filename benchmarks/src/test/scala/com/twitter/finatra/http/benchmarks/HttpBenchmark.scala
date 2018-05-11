package com.twitter.finatra.http.benchmarks

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future

trait HttpBenchmark {

  protected def defaultCallback(request: Request): Future[Response] =
    Future.value(Response())
}
