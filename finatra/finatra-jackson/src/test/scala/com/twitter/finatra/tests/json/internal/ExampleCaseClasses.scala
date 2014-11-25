package com.twitter.finatra.tests.json.internal

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties}
import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.json.{WrappedValue, ValidationResult}

case class CaseClass(id: Long, name: String)

case class CaseClassWithLazyVal(id: Long) {
  lazy val woo = "yeah"
}

case class CaseClassWithIgnoredField(id: Long) {
  @JsonIgnore
  val uncomfortable = "Bad Touch"
}

@JsonIgnoreProperties(Array("uncomfortable", "unpleasant"))
case class CaseClassWithIgnoredFields(id: Long) {
  val uncomfortable = "Bad Touch"
  val unpleasant = "The Creeps"
}

case class CaseClassWithTransientField(id: Long) {
  @transient
  val lol = "I'm sure it's just a phase."
}

case class CaseClassWithLazyField(id: Long) {
  lazy val lol = "I'm sure it's just a phase."
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
