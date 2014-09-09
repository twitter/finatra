package com.twitter.finatra.integration

import com.twitter.finatra.utils.Logging
import javax.inject.Inject

class SampleManager @Inject()(
  sampleService: SampleService)
  extends Logging {

  def start() {
    info("started")
    sampleService.sayHi("yo")
  }
}
