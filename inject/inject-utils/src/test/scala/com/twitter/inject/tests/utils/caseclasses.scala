package com.twitter.inject.tests.utils

import com.twitter.inject.annotations._

case class CaseClassOneTwo(@Annotation1 one: String, @Annotation2 two: String)
case class CaseClassOneTwoWithFields(@Annotation1 one: String, @Annotation2 two: String) {
  val city: String = "San Francisco"
  val state: String = "California"
}
case class CaseClassOneTwoWithAnnotatedField(@Annotation1 one: String, @Annotation2 two: String) {
  @Annotation3 val three: String =
    "three" // will not be returned AnnotationUtils.findAnnotations which only finds constructor annotations
}
case class CaseClassThreeFour(@Annotation3 three: String, @Annotation4 four: String)
case class CaseClassOneTwoThreeFour(
  @Annotation1 one: String,
  @Annotation2 two: String,
  @Annotation3 three: String,
  @Annotation4 four: String)

case class WithThings(
  @Annotation1 @Thing("thing1") thing1: String,
  @Annotation2 @Thing("thing2") thing2: String)
case class WithWidgets(
  @Annotation3 @Widget("widget1") widget1: String,
  @Annotation4 @Widget("widget2") widget2: String)

case class WithSecondaryConstructor(
  @Annotation1 one: Int,
  @Annotation2 two: Int) {
  def this(@Annotation3 three: String, @Annotation4 four: String) {
    this(three.toInt, four.toInt)
  }
}

object StaticSecondaryConstructor {
  def apply(@Annotation3 three: String, @Annotation4 four: String): StaticSecondaryConstructor =
    StaticSecondaryConstructor(three.toInt, four.toInt)
}
case class StaticSecondaryConstructor(@Annotation1 one: Int, @Annotation2 two: Int)

object StaticSecondaryConstructorWithMethodAnnotation {
  def apply(@Annotation3 three: String, @Annotation4 four: String): StaticSecondaryConstructor =
    StaticSecondaryConstructor(three.toInt, four.toInt)
}
case class StaticSecondaryConstructorWithMethodAnnotation(
  @Annotation1 one: Int,
  @Annotation2 two: Int) {
  @Widget("widget1") def widget1: String = "this is widget 1 method"
}

case class GenericTestCaseClass[T](@Annotation1 one: T)
case class GenericTestCaseClassWithMultipleArgs[T](@Annotation1 one: T, @Annotation2 two: Int)
