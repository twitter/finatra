package com.twitter.inject.tests

import com.google.inject.{Guice, Key}
import com.twitter.inject.tests.module.{ClassToConvert, ComplexServiceFactory, DoEverythingModule}
import com.twitter.inject.{Injector, Test}

class TwitterModuleTest extends Test {

  val injector =
    Injector(Guice.createInjector(DoEverythingModule))

  test("get assisted factory instance from injector") {
    assertServiceFactory(injector.instance[ComplexServiceFactory])

    assertServiceFactory(injector.instance(classOf[ComplexServiceFactory]))

    assertServiceFactory(injector.instance(Key.get(classOf[ComplexServiceFactory])))
  }

  test("get additional instances from injector") {
    injector.instance[String](name = "str1") should be("string1")

    injector.instance[String, Prod] should be("prod string")
  }

  test("type conversion") {
    injector.instance[ClassToConvert](name = "name") should be(ClassToConvert("Steve"))
  }

  def assertServiceFactory(complexServiceFactory: ComplexServiceFactory): Unit = {
    complexServiceFactory.create("foo").execute should equal("done foo")
  }
}
