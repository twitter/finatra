package com.twitter.inject.tests

import com.google.inject.Guice
import com.twitter.inject.Test
import com.twitter.inject.tests.module.{ComplexServiceFactory, DoEverythingModule}

class TwitterModuleTest extends Test {

  "get assisted factory instance from injector" in {
    val injector = Guice.createInjector(DoEverythingModule)
    val complexServiceFactory = injector.getInstance(classOf[ComplexServiceFactory])
    complexServiceFactory.create("foo").execute should equal("done foo")
  }
}
