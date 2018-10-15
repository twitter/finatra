package com.twitter.finatra.json.tests

import com.google.inject.spi.Message
import com.google.inject.{ConfigurationException, Injector, Key}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.JsonInjectException
import com.twitter.finatra.json.tests.internal._
import com.twitter.inject.{Mockito, Test}
import java.util
import net.codingwell.scalaguice.typeLiteral

class GuiceInjectableValuesFinatraObjectMapperTest extends Test with Mockito {

  val injector = mock[Injector]

  /* Class under test */
  val mapper = FinatraObjectMapper.create(injector)

  override def afterEach() = {
    super.afterEach()
    reset(injector)
  }

  test("@Inject value should work when field not sent in json") {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) returns "Foo"
    assert(parse[CaseClassInjectString]("""
      {
      }
      """) == CaseClassInjectString("Foo"))
  }

  test("@Inject Guice Option[String] into case class Option[String]") {
    val key = Key.get(typeLiteral[Option[String]])
    injector.getInstance(key) returns Some("Foo")
    assert(parse[CaseClassInjectOptionString]("""
      {
      }
      """) == CaseClassInjectOptionString(Some("Foo")))
  }

  test("@Inject value should use default when field not sent in json") {
    assert(parse[CaseClassInjectStringWithDefault]("""
      {
      }
      """) == CaseClassInjectStringWithDefault("DefaultHello"))
  }

  test("@Inject value should use default when field sent in json") {
    assert(parse[CaseClassInjectStringWithDefault]("""
      {
        "string": "123"
      }
      """) == CaseClassInjectStringWithDefault("DefaultHello"))
  }

  test("@Inject value should use None assumed default when field sent in json") {
    assert(parse[CaseClassInjectOptionString]("""
      {
        "string": "123"
      }
      """) == CaseClassInjectOptionString(None))
  }

  test("@Inject value takes precedence over value in json") {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) returns "Foo"
    assert(parse[CaseClassInjectString]("""
      {
        "string": "123"
      }""") == CaseClassInjectString("Foo"))
  }

  test("@Inject ConfigurationException") {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) throws new ConfigurationException(
      new util.LinkedList[Message]()
    )

    intercept[JsonInjectException] {
      parse[CaseClassInjectString]("""
        {
          "string": "123"
        }""")
    }
  }

  test("Too many injectable annotations") {
    intercept[AssertionError] {
      parse[CaseClassTooManyInjectableAnnotations]("""{}""")
    }
  }

  private def parse[T: Manifest](string: String): T = {
    mapper.parse[T](string)
  }
}
