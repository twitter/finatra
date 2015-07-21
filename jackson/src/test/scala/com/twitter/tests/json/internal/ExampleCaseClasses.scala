package com.twitter.finatra.tests.json.internal

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.domain.WrappedValue
import com.twitter.finatra.request._
import com.twitter.finatra.validation.{NotEmpty, ValidationResult}
import com.twitter.inject.Logging
import org.joda.time.DateTime
import scala.annotation.meta.param

case class CaseClass(id: Long, name: String)

case class CaseClassWithLazyVal(id: Long) {
  lazy val woo = "yeah"
}

case class CaseClassWithIgnoredField(id: Long) {
  @JsonIgnore
  val ignoreMe = "Foo"
}

@JsonIgnoreProperties(Array("ignore_me", "feh"))
case class CaseClassWithIgnoredFieldsMatchAfterToSnakeCase(id: Long) {
  val ignoreMe = "Foo"
  val feh = "blah"
}

@JsonIgnoreProperties(Array("ignore_me", "feh"))
case class CaseClassWithIgnoredFieldsExactMatch(id: Long) {
  val ignore_me = "Foo"
  val feh = "blah"
}

case class CaseClassWithTransientField(id: Long) {
  @transient
  val lol = "asdf"
}

case class CaseClassWithLazyField(id: Long) {
  lazy val lol = "asdf"
}

case class CaseClassWithOverloadedField(id: Long) {
  def id(prefix: String): String = prefix + id
}

case class CaseClassWithOption(value: Option[String] = None)

case class CaseClassWithJsonNode(value: JsonNode)

case class CaseClassWithAllTypes(map: Map[String, String],
  set: Set[Int],
  string: String,
  list: List[Int],
  seq: Seq[Int],
  indexedSeq: IndexedSeq[Int],
  vector: Vector[Int],
  bigDecimal: BigDecimal,
  bigInt: Int, //TODO: BigInt,
  int: Int,
  long: Long,
  char: Char,
  bool: Boolean,
  short: Short,
  byte: Byte,
  float: Float,
  double: Double,
  any: Any,
  anyRef: AnyRef,
  intMap: Map[Int, Int] = Map(),
  longMap: Map[Long, Long] = Map())

case class CaseClassWithException() {
  throw new NullPointerException("Oops!!!")
}

object OuterObject {

  case class NestedCaseClass(id: Long)

  object InnerObject {

    case class SuperNestedCaseClass(id: Long)

  }

}

case class CaseClassWithTwoConstructors(id: Long, name: String) {
  def this(id: Long) = this(id, "New User")
}

case class CaseClassWithSnakeCase(oneThing: String, twoThing: String)

case class CaseClassWithArrays(
  one: String,
  two: Array[String],
  three: Array[Int],
  four: Array[Long],
  five: Array[Char])

case class CaseClassWithArrayLong(array: Array[Long])

case class CaseClassWithSeqLong(seq: Seq[Long])

case class Foo(name: String)

case class Car(
  id: Long,
  make: CarMake,
  model: String,
  passengers: Seq[Person]) {

  def validateId = {
    ValidationResult(
      id > 0,
      "id must be > 0")
  }
}

case class Person(
  id: Int,
  name: String,
  age: Option[Int],
  age_with_default: Option[Int] = None,
  nickname: String = "unknown")

case class PersonWithDottedName(
  id: Int,
  @JsonProperty("name.last") lastName: String)

case class SimplePerson(name: String)

case class CaseClassWithMap(map: Map[String, String])

case class CaseClassWithSetOfLongs(set: Set[Long])

case class CaseClassWithSeqOfLongs(seq: Seq[Long])

case class CaseClassWithNestedSeqLong(
  seqClass: CaseClassWithSeqLong,
  setClass: CaseClassWithSetOfLongs)

case class Blah(foo: String)

case class TestIdStringWrapper(id: String)
  extends WrappedValue[String]

case class ObjWithTestId(id: TestIdStringWrapper)

object Obj {

  case class NestedCaseClassInObject(id: String)

}


case class WrappedValueInt(value: Int)
  extends WrappedValue[Int]

case class WrappedValueLong(value: Long)
  extends WrappedValue[Long]

case class WrappedValueString(value: String)
  extends WrappedValue[String]

case class WrappedValueIntInObj(
  foo: WrappedValueInt)

case class WrappedValueStringInObj(
  foo: WrappedValueString)

case class WrappedValueLongInObj(
  foo: WrappedValueLong)

case class CaseClassWithVal(
  name: String) {

  val `type`: String = "person"
}

case class CaseClassWithEnum(
  name: String,
  make: CarMakeEnum)

case class CaseClassWithComplexEnums(
  name: String,
  make: CarMakeEnum,
  makeOpt: Option[CarMakeEnum],
  makeSeq: Seq[CarMakeEnum],
  makeSet: Set[CarMakeEnum])

case class CaseClassWithSeqEnum(
  enumSeq: Seq[CarMakeEnum])

case class CaseClassWithOptionEnum(
  enumOpt: Option[CarMakeEnum])

case class CaseClassWithDateTime(
  dateTime: DateTime)

case class CaseClassWithIntAndDateTime(
  @NotEmpty name: String,
  age: Int,
  age2: Int,
  age3: Int,
  dateTime: DateTime,
  dateTime2: DateTime,
  dateTime3: DateTime,
  dateTime4: DateTime,
  @NotEmpty dateTime5: Option[DateTime])

case class ClassWithFooClassInject(
  @RequestInject fooClass: FooClass)

case class ClassWithQueryParamDateTimeInject(
  @QueryParam dateTime: DateTime)

case class CaseClassWithEscapedLong(
  `1-5`: Long)

case class CaseClassWithEscapedString(
  `1-5`: String)

case class CaseClassWithEscapedNormalString(
  `a`: String)

case class UnicodeNameCaseClass(`winning-id`: Int, name: String)

case class TestEntityIdsResponse(
  entityIds: Seq[Long],
  previousCursor: String,
  nextCursor: String)

object TestEntityIdsResponseWithCompanion {
  val msg = "im the companion"
}

case class TestEntityIdsResponseWithCompanion(
  entityIds: Seq[Long],
  previousCursor: String,
  nextCursor: String)

case class WrappedValueStringMapObject(
  map: Map[WrappedValueString, String])

case class FooClass(id: String)

case class Group3(id: String)
  extends Logging

case class CaseClassWithInvalidValidation(
  @(InvalidValidationInternal@param) name: String,
  make: CarMakeEnum)

case class NoConstructorArgs()

case class CaseClassWithBoolean(foo: Boolean)