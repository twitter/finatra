package com.twitter.finatra.json.tests.internal

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonProperty, JsonValue}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ValueNode
import com.twitter.finatra.request._
import com.twitter.finatra.response.JsonCamelCase
import com.twitter.finatra.validation.ValidationResult
import com.twitter.finatra.validation.{Min, NotEmpty, Size}
import com.twitter.inject.Logging
import com.twitter.inject.domain.WrappedValue
import com.twitter.{util => ctu}
import javax.inject.Inject
import org.joda.time.DateTime
import scala.annotation.meta.param
import scala.math.BigDecimal.RoundingMode

sealed trait CarType {
  @JsonValue
  def toJson: String
}
object Volvo extends CarType {
  override def toJson: String = "volvo"
}
object Audi extends CarType {
  override def toJson: String = "audi"
}
object Volkswagen extends CarType {
  override def toJson: String = "vw"
}

case class Vehicle(vin: String, `type`: CarType)

case class CaseClass(id: Long, name: String)

case class CaseClassWithLazyVal(id: Long) {
  lazy val woo = "yeah"
}

case class GenericTestCaseClass[T](data: T)

case class Page[T](data: List[T], pageSize: Int, next: Option[Long], previous: Option[Long])

case class CaseClassWithTypes[T, U](first: T, second: U)

case class CaseClassWithMapTypes[T, U](data: Map[T, U])

case class CaseClassWithManyTypes[R, S, T](one: R, two: S, three: T)

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

case class CaseClassWithAllTypes(
  map: Map[String, String],
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
  longMap: Map[Long, Long] = Map()
)

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
  five: Array[Char],
  bools: Array[Boolean],
  bytes: Array[Byte],
  doubles: Array[Double],
  floats: Array[Float]
)

case class CaseClassWithArrayLong(array: Array[Long])

case class CaseClassWithArrayListOfIntegers(arraylist: java.util.ArrayList[java.lang.Integer])

case class CaseClassWithArrayBoolean(array: Array[Boolean])

case class CaseClassWithArrayWrappedValueLong(array: Array[WrappedValueLong])

case class CaseClassWithSeqLong(seq: Seq[Long])

case class CaseClassWithSeqWrappedValueLong(seq: Seq[WrappedValueLong])

case class CaseClassWithValidation(@Min(1) value: Long)

case class CaseClassWithSeqOfCaseClassWithValidation(seq: Seq[CaseClassWithValidation])

case class WrappedValueLongWithValidation(@Min(1) value: Long) extends WrappedValue[Long]

case class CaseClassWithSeqWrappedValueLongWithValidation(seq: Seq[WrappedValueLongWithValidation])

case class Foo(name: String)

case class CaseClassCharacter(c: Char)

case class Car(id: Long, make: CarMake, model: String, passengers: Seq[Person]) {

  def validateId: ValidationResult = {
    ValidationResult.validate(id > 0, "id must be > 0")
  }
}

case class Person(
  id: Int,
  name: String,
  age: Option[Int],
  age_with_default: Option[Int] = None,
  nickname: String = "unknown"
)

case class PersonWithLogging(
  id: Int,
  name: String,
  age: Option[Int],
  age_with_default: Option[Int] = None,
  nickname: String = "unknown"
) extends Logging

case class PersonWithDottedName(id: Int, @JsonProperty("name.last") lastName: String)

case class SimplePerson(name: String)

case class PersonWithThings(
  id: Int,
  name: String,
  age: Option[Int],
  @Size(min = 1, max = 10) things: Map[String, Things]
)

case class Things(
  @Size(min = 1, max = 2) names: Seq[String])

@JsonCamelCase
case class CamelCaseSimplePerson(myName: String)

case class CamelCaseSimplePersonNoAnnotation(myName: String)

case class CaseClassWithMap(map: Map[String, String])

case class CaseClassWithSortedMap(sortedMap: scala.collection.SortedMap[String, Int])

case class CaseClassWithSetOfLongs(set: Set[Long])

case class CaseClassWithSeqOfLongs(seq: Seq[Long])

case class CaseClassWithNestedSeqLong(
  seqClass: CaseClassWithSeqLong,
  setClass: CaseClassWithSetOfLongs
)

case class Blah(foo: String)

case class TestIdStringWrapper(id: String) extends WrappedValue[String]

case class ObjWithTestId(id: TestIdStringWrapper)

object Obj {

  case class NestedCaseClassInObject(id: String)

  case class NestedCaseClassInObjectWithNestedCaseClassInObjectParam(
    nested: NestedCaseClassInObject
  )

}

class TypeAndCompanion
object TypeAndCompanion {
  case class NestedCaseClassInCompanion(id: String)
}

case class WrappedValueInt(value: Int) extends WrappedValue[Int]

case class WrappedValueLong(value: Long) extends WrappedValue[Long]

case class WrappedValueString(value: String) extends WrappedValue[String]

case class WrappedValueIntInObj(foo: WrappedValueInt)

case class WrappedValueStringInObj(foo: WrappedValueString)

case class WrappedValueLongInObj(foo: WrappedValueLong)

case class CaseClassWithVal(name: String) {

  val `type`: String = "person"
}

case class CaseClassWithEnum(name: String, make: CarMakeEnum)

case class CaseClassWithComplexEnums(
  name: String,
  make: CarMakeEnum,
  makeOpt: Option[CarMakeEnum],
  makeSeq: Seq[CarMakeEnum],
  makeSet: Set[CarMakeEnum]
)

case class CaseClassWithSeqEnum(enumSeq: Seq[CarMakeEnum])

case class CaseClassWithOptionEnum(enumOpt: Option[CarMakeEnum])

case class CaseClassWithDateTime(dateTime: DateTime)

case class CaseClassWithIntAndDateTime(
  @NotEmpty name: String,
  age: Int,
  age2: Int,
  age3: Int,
  dateTime: DateTime,
  dateTime2: DateTime,
  dateTime3: DateTime,
  dateTime4: DateTime,
  @NotEmpty dateTime5: Option[DateTime]
)

case class CaseClassWithTwitterUtilDuration(duration: ctu.Duration)

case class ClassWithFooClassInject(@Inject fooClass: FooClass)

case class ClassWithQueryParamDateTimeInject(@QueryParam dateTime: DateTime)

case class CaseClassWithEscapedLong(`1-5`: Long)

case class CaseClassWithEscapedString(`1-5`: String)

case class CaseClassWithEscapedNormalString(`a`: String)

case class UnicodeNameCaseClass(`winning-id`: Int, name: String)

case class TestEntityIdsResponse(entityIds: Seq[Long], previousCursor: String, nextCursor: String)

object TestEntityIdsResponseWithCompanion {
  val msg = "im the companion"
}

case class TestEntityIdsResponseWithCompanion(
  entityIds: Seq[Long],
  previousCursor: String,
  nextCursor: String
)

case class WrappedValueStringMapObject(map: Map[WrappedValueString, String])

case class FooClass(id: String)

case class Group3(id: String) extends Logging

case class CaseClassWithInvalidValidation(
  @(InvalidValidationInternal @param) name: String,
  make: CarMakeEnum
)

case class NoConstructorArgs()

case class CaseClassWithBoolean(foo: Boolean)

case class CaseClassWithSeqBooleans(foos: Seq[Boolean])

case class CaseClassInjectStringWithDefault(@Inject string: String = "DefaultHello")

case class CaseClassInjectInt(@Inject age: Int)

case class CaseClassInjectOptionInt(@Inject age: Option[Int])

case class CaseClassInjectOptionString(@Inject string: Option[String])

case class CaseClassInjectString(@Inject string: String)

case class CaseClassTooManyInjectableAnnotations(@Inject @QueryParam string: String)

case class CaseClassWithCustomDecimalFormat(
  @JsonDeserialize(using = classOf[MyBigDecimalDeserializer])
  myBigDecimal: BigDecimal,
  @JsonDeserialize(using = classOf[MyBigDecimalDeserializer])
  optMyBigDecimal: Option[BigDecimal]
)

case class CaseClassWithLongAndDeserializer(
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  long: Long
)

case class CaseClassWithOptionLongAndDeserializer(
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  optLong: Option[Long]
)

class MyBigDecimalDeserializer extends JsonDeserializer[BigDecimal] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): BigDecimal = {
    val jsonNode: ValueNode = jp.getCodec.readTree(jp)
    BigDecimal(jsonNode.asText).setScale(2, RoundingMode.HALF_UP)
  }

  override def getEmptyValue: BigDecimal = BigDecimal(0)
}

case class WithEmptyJsonProperty(@JsonProperty foo: String)

case class WithNonemptyJsonProperty(@JsonProperty("bar") foo: String)

case class WithoutJsonPropertyAnnotation(foo: String)

case class NamingStrategyJsonProperty(@JsonProperty longFieldName: String)

trait CaseClassTrait {
  @JsonProperty("fedoras")
  @Size(min = 1, max = 2)
  def names: Seq[String]

  @Min(1L)
  def age: Int
}
case class CaseClassTraitImpl(
  names: Seq[String],
  @JsonProperty("oldness") age: Int
) extends CaseClassTrait

package object internal {

  case class SimplePersonInPackageObject( // not recommended but used here for testing use case
    name: String = "default-name"
  )

  case class SimplePersonInPackageObjectWithoutConstructorParams() // not recommended but used here for testing use case

}
