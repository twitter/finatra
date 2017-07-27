package com.twitter.inject.tests.module

import com.google.inject.assistedinject.Assisted
import javax.inject.{Inject, Named}

class ComplexService @Inject()(
  exampleService: DoEverythingService,
  defaultString: String,
  @Named("str1") string1: String,
  @Named("str2") string2: String,
  defaultInt: Int,
  @Assisted name: String
) {

  def execute = {
    exampleService.doit + " " + name
  }
}
