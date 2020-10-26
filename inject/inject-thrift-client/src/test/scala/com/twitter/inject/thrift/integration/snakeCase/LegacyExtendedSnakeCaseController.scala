package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finatra.thrift.Controller
import com.twitter.snakeCase.thriftscala.ExtendedSnakeCaseService
import com.twitter.snakeCase.thriftscala.ExtendedSnakeCaseService.AdditionalEvent
import com.twitter.snakeCase.thriftscala.SnakeCaseService.{DequeueEvent, EnqueueEvent}
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class LegacyExtendedSnakeCaseController
    extends Controller
    with ExtendedSnakeCaseService.BaseServiceIface {
  override val additionalEvent = handle(AdditionalEvent) { _ =>
    Future.value(true)
  }

  override val enqueueEvent = handle(EnqueueEvent) { _ =>
    Future.value(true)
  }

  override val dequeueEvent = handle(DequeueEvent) { _ =>
    Future.value(true)
  }
}
