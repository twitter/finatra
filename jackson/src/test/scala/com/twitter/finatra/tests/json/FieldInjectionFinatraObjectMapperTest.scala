package com.twitter.finatra.tests.json

import com.fasterxml.jackson.databind.{BeanProperty, DeserializationContext, InjectableValues}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.tests.json.internal._
import com.twitter.inject.{Logging, Mockito}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FeatureSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class FieldInjectionFinatraObjectMapperTest
  extends FeatureSpec
  with Matchers
  with Mockito
  with Logging {

  /* Class under test */
  val objectMapper = FinatraObjectMapper.create()
  val injectableValues = smartMock[InjectableValues]

  def afterEach() = {
    reset(injectableValues)
  }

  scenario("Simulate @Query injection of String into Int") {
    injectableValues.findInjectableValue(
      any[Object], any[DeserializationContext], any[BeanProperty], any[Object]) returns "123"

    val injectableObjectMapper = objectMapper.reader[CaseClassInjectInt].`with`(injectableValues)
    injectableObjectMapper.readValue[CaseClassInjectInt]("{}") should be(CaseClassInjectInt(123))
  }

  scenario("Simulate @Query injection of String into Option[Int]") {
    injectableValues.findInjectableValue(
      any[Object], any[DeserializationContext], any[BeanProperty], any[Object]) returns "123"

    val injectableObjectMapper = objectMapper.reader[CaseClassInjectOptionInt].`with`(injectableValues)
    injectableObjectMapper.readValue[CaseClassInjectOptionInt]("{}") should be(CaseClassInjectOptionInt(Some(123)))
  }

  scenario("Simulate @Query injection of String into String") {
    injectableValues.findInjectableValue(
      any[Object], any[DeserializationContext], any[BeanProperty], any[Object]) returns "123"

    val injectableObjectMapper = objectMapper.reader[CaseClassInjectString].`with`(injectableValues)
    injectableObjectMapper.readValue[CaseClassInjectString]("{}") should be(CaseClassInjectString("123"))
  }

  scenario("Simulate @Query injection of String into Option[String]") {
    injectableValues.findInjectableValue(
      any[Object], any[DeserializationContext], any[BeanProperty], any[Object]) returns "123"

    val injectableObjectMapper = objectMapper.reader[CaseClassInjectOptionString].`with`(injectableValues)
    injectableObjectMapper.readValue[CaseClassInjectOptionString]("{}") should be(CaseClassInjectOptionString(Some("123")))
  }
}

