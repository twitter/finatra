package com.twitter.inject.app.tests

import com.twitter.inject.Logging
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
