package com.twitter.inject.tests

import com.google.inject.{Guice, Key}
import com.twitter.finatra.tests.Prod
import com.twitter.inject.tests.module.{ClassToConvert, ComplexServiceFactory, DoEverythingModule}
import com.twitter.inject.{Injector, Test}

class TwitterModuleTest extends Test {

  val guiceInjector = Guice.createInjector(DoEverythingModule)
  val injector = Injector(guiceInjector)

  "get assisted factory instance from injector" in {
    assertServiceFactory(
      injector.instance[ComplexServiceFactory])

    assertServiceFactory(
      injector.instance(classOf[ComplexServiceFactory]))

    assertServiceFactory(
      injector.instance(Key.get(classOf[ComplexServiceFactory])))
  }

  "get additional instances from injector" in {
    injector.instance[String](name = "str1") should be("string1")

    injector.instance[String, Prod] should be("prod string")
  }

  "type conversion" in {
    injector.instance[ClassToConvert](name = "name") should be(ClassToConvert("Steve"))
  }

  def assertServiceFactory(complexServiceFactory: ComplexServiceFactory): Unit = {
    complexServiceFactory.create("foo").execute should equal("done foo")
  }
}
