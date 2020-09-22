package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finatra.thrift.Controller
import com.twitter.scrooge.Response
import com.twitter.snakeCase.thriftscala.{ExtendedSnakeCaseService, SnakeCaseService}
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class ExtendedSnakeCaseController extends Controller(ExtendedSnakeCaseService) {

  handle(SnakeCaseService.EnqueueEvent).withFn { _ =>
    Future.value(
      Response(true)
    )
  }

  handle(SnakeCaseService.DequeueEvent).withFn { _ =>
    Future.value(
      Response(true)
    )
  }

  handle(ExtendedSnakeCaseService.AdditionalEvent).withFn { _ =>
    Future.value(
      Response(true)
    )
  }

}
