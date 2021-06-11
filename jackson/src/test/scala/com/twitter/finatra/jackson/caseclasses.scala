package com.twitter.finatra.jackson

import com.fasterxml.jackson.annotation._
import javax.inject.Inject
import org.joda.time.DateTime

sealed trait ZeroOrOne
object Zero extends ZeroOrOne
object One extends ZeroOrOne
object Two extends ZeroOrOne

case class CaseClassWithZeroOrOne(id: ZeroOrOne)

case class CaseClass(id: Long, name: String)

case class CaseClassIdAndOption(id: Long, name: Option[String])

@JsonIgnoreProperties(ignoreUnknown = false)
case class StrictCaseClassIdAndOption(id: Long, name: Option[String])

case class FooClass(id: String)

@JsonIgnoreProperties(ignoreUnknown = false)
case class StrictCaseClass(id: Long, name: String)

@JsonIgnoreProperties(ignoreUnknown = true)
case class LooseCaseClass(id: Long, name: String)

case class ClassWithFooClassInject(@Inject fooClass: FooClass)

case class SimpleClassWithInjection(@TestInjectableValue(value = "accept") hello: String)

case class ClassWithQueryParamDateTimeInject(@TestInjectableValue dateTime: DateTime)

case class ClassWithFooClassInjectAndDefault(@Inject fooClass: FooClass = FooClass("12345"))

case class CaseClassWithOption(value: Option[String] = None)

@JsonIgnoreProperties(ignoreUnknown = false)
case class StrictCaseClassWithOption(value: Option[String] = None)

case class CamelCaseSimplePersonNoAnnotation(myName: String)

case class CaseClassWithSeqBooleans(foos: Seq[Boolean])

case class CaseClassInjectStringWithDefault(@Inject string: String = "DefaultHello")

case class CaseClassInjectInt(@Inject age: Int)

case class CaseClassInjectOptionInt(@Inject age: Option[Int])

case class CaseClassInjectOptionString(@Inject string: Option[String])

case class CaseClassInjectString(@Inject string: String)

case class CaseClassTooManyInjectableAnnotations(@Inject @TestInjectableValue string: String)

case class CaseClassWithManyAnnotationsThatShouldProbablyBeAcceptable(
  @JsonIgnore @Inject string: String)

case class CaseClassWithManyAnnotationsFail(@JsonProperty("whatever") @Inject string: String)
