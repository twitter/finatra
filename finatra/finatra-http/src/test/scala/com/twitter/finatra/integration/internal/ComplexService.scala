package com.twitter.finatra.integration.internal

import com.google.inject.assistedinject.Assisted
import com.twitter.finatra.annotations.Flag
import javax.inject.Inject
import org.joda.time.Duration

class ComplexService @Inject()(
  exampleService: DoEverythingService,
  @Flag("moduleDuration") duration1: Duration,
  @Assisted name: String) {

  def execute = {
    exampleService.doit + " " + name + " " + duration1.getMillis
  }
}