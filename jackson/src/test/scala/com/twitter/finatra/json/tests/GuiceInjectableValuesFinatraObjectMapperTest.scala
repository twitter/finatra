package com.twitter.finatra.json.tests

import com.google.inject.spi.Message
import com.google.inject.{ConfigurationException, Injector, Key}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.JsonInjectException
import com.twitter.finatra.json.tests.internal._
import com.twitter.inject.{Mockito, Test}
import java.util
import net.codingwell.scalaguice.typeLiteral

class GuiceInjectableValuesFinatraObjectMapperTest
  extends Test
  with Mockito {

  val injector = mock[Injector]

  /* Class under test */
  val mapper = FinatraObjectMapper.create(injector)

  override def afterEach() = {
    super.afterEach()
    reset(injector)
  }

  "@Inject value should work when field not sent in json" in {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) returns "Foo"
    assert(parse[CaseClassInjectString](
      """
      {
      }
      """) == CaseClassInjectString("Foo"))
  }

  "@Inject Guice Option[String] into case class Option[String]" in {
    val key = Key.get(typeLiteral[Option[String]])
    injector.getInstance(key) returns Some("Foo")
    assert(parse[CaseClassInjectOptionString](
      """
      {
      }
      """) == CaseClassInjectOptionString(Some("Foo")))
  }

  "@Inject value should use default when field not sent in json" in {
    assert(parse[CaseClassInjectStringWithDefault](
      """
      {
      }
      """) == CaseClassInjectStringWithDefault("DefaultHello"))
  }

  "@Inject value should use default when field sent in json" in {
    assert(parse[CaseClassInjectStringWithDefault](
      """
      {
        "string": "123"
      }
      """) == CaseClassInjectStringWithDefault("DefaultHello"))
  }

  "@Inject value should use None assumed default when field sent in json" in {
    assert(parse[CaseClassInjectOptionString](
      """
      {
        "string": "123"
      }
      """) == CaseClassInjectOptionString(None))
  }

  "@Inject value takes precedence over value in json" in {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) returns "Foo"
    assert(parse[CaseClassInjectString]("""
      {
        "string": "123"
      }""") == CaseClassInjectString("Foo"))
  }

  "@Inject ConfigurationException" in {
    val keyString = Key.get(classOf[String])
    injector.getInstance(keyString) throws new ConfigurationException(new util.LinkedList[Message]())

    intercept[JsonInjectException] {
      parse[CaseClassInjectString]("""
        {
          "string": "123"
        }""")
    }
  }

  "Too many injectable annotations" in {
    intercept[AssertionError] {
      parse[CaseClassTooManyInjectableAnnotations]("""{}""")
    }
  }

  "Too many binding annotations" in {
    intercept[Exception] {
      parse[CaseClassTooManyBindingAnnotations]("""{}""")
    }
  }

  private def parse[T: Manifest](string: String): T = {
    mapper.parse[T](string)
  }
}
