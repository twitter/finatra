package com.twitter.finatra.integration.app

import com.twitter.finatra.utils.Logging
import javax.inject.Inject

class SampleManager @Inject()(
  sampleService: SampleService)
  extends Logging {

  def start() {
    info("SampleManager started")
    val response = sampleService.sayHi("yo")
    info("Service said " + response)
  }
}
