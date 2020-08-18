package com.twitter.finatra.jackson.tests.caseclass

import com.google.inject.spi.Message
import com.google.inject.{ConfigurationException, Injector, Key}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.caseclass.exceptions.InjectableValuesException
import com.twitter.finatra.jackson.tests.{
  CaseClassInjectOptionString,
  CaseClassInjectString,
  CaseClassInjectStringWithDefault,
  CaseClassTooManyInjectableAnnotations,
  CaseClassWithManyAnnotationsFail,
  CaseClassWithManyAnnotationsThatShouldProbablyBeAcceptable
}
import com.twitter.inject.Test
import com.twitter.mock.Mockito
import net.codingwell.scalaguice.typeLiteral

class DefaultInjectableValuesTest extends Test with Mockito {
  private[this] val injector: Injector = mock[Injector]

  /* Class under test */
  private[this] val mapper: ScalaObjectMapper = ScalaObjectMapper(injector)

  override def afterEach(): Unit = {
    super.afterEach()
    reset(injector)
  }

  test("@Inject value should work when field not sent in json") {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) returns "Foo"
    parse[CaseClassInjectString]("""{}""") should equal(CaseClassInjectString("Foo"))
  }

  test("@Inject Option[String] value into case class") {
    val key = Key.get(typeLiteral[Option[String]])
    injector.getInstance(key) returns Some("Foo")
    parse[CaseClassInjectOptionString]("""{}""") should equal(
      CaseClassInjectOptionString(Some("Foo")))
  }

  test("@Inject value should use default when field not sent in json") {
    parse[CaseClassInjectStringWithDefault]("""{}""") should equal(
      CaseClassInjectStringWithDefault("DefaultHello"))
  }

  test("@Inject value should use default when field sent in json") {
    parse[CaseClassInjectStringWithDefault]("""{"string": "123"}""") should equal(
      CaseClassInjectStringWithDefault("DefaultHello"))
  }

  test("@Inject value should use None assumed default when field sent in json") {
    parse[CaseClassInjectOptionString]("""{"string": "123"}""") should equal(
      CaseClassInjectOptionString(None))
  }

  test("@Inject value takes precedence over value in json") {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) returns "Foo"
    parse[CaseClassInjectString]("""{"string": "123"}""") should equal(CaseClassInjectString("Foo"))
  }

  test("@Inject ConfigurationException") {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) throws new ConfigurationException(
      new java.util.LinkedList[Message]()
    )

    intercept[InjectableValuesException] {
      parse[CaseClassInjectString]("""{"string": "123"}""")
    }
  }

  test("Too many injectable annotations") {
    intercept[AssertionError] {
      parse[CaseClassTooManyInjectableAnnotations]("""{}""")
    }
  }

  test("many annotations -- even with JsonIgnore, fails") {
    intercept[AssertionError] {
      parse[CaseClassWithManyAnnotationsThatShouldProbablyBeAcceptable]("""{"string": "123"}""")
    }
  }

  test("many annotations -- fail") {
    intercept[AssertionError] {
      parse[CaseClassWithManyAnnotationsFail]("""{}""")
    }
  }

  private def parse[T: Manifest](string: String): T = {
    mapper.parse[T](string)
  }
}
