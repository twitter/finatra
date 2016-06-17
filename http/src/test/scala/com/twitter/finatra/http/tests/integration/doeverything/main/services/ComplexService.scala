package com.twitter.finatra.http.tests.integration.doeverything.main.services

import com.google.inject.assistedinject.Assisted
import com.twitter.inject.annotations.Flag
import javax.inject.{Inject, Named}
import org.joda.time.Duration

class ComplexService @Inject()(
  exampleService: DoEverythingService,
  defaultString: String,
  @Named("str1") string1: String,
  @Named("str2") string2: String,
  defaultInt: Int,
  @Flag("moduleDuration") duration1: Duration,
  @Assisted name: String) {

  def execute = {
    exampleService.doit + " " + name + " " + duration1.getMillis
  }
}
