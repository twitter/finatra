package com.twitter.finatra.json.tests.internal.caseclass.jackson

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.BigDecimalDeserializer
import com.twitter.finatra.json.internal.caseclass.jackson.{CaseClassField, NullCaseClassValidationProvider}
import com.twitter.finatra.json.tests.internal.{WithEmptyJsonProperty, WithNonemptyJsonProperty, WithoutJsonPropertyAnnotation}
import com.twitter.finatra.request.{Header, QueryParam}
import com.twitter.finatra.validation.{NotEmpty, NotEmptyInternal}
import com.twitter.inject.Test

class CaseClassFieldTest extends Test {

  test("CaseClassField.createFields have field name foo") {
    val fields = CaseClassField.createFields(
      classOf[WithEmptyJsonProperty],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    fields.head.name should equal("foo")
  }

  test("CaseClassField.createFields also have field name foo") {
    val fields = CaseClassField.createFields(
      classOf[WithoutJsonPropertyAnnotation],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    fields.head.name should equal("foo")
  }

  test("CaseClassField.createFields have field name bar") {
    val fields = CaseClassField.createFields(
      classOf[WithNonemptyJsonProperty],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    fields.head.name should equal("bar")
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation") {
    val fields = CaseClassField.createFields(
      classOf[Aum],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(2)

    val iField = fields.head
    iField.name should equal("i")
    iField.annotations.size should equal(1)
    iField.annotations.head.annotationType() should be(classOf[JsonProperty])

    val jField = fields.last
    jField.name should equal("j")
    jField.annotations.size should equal(1)
    jField.annotations.head.annotationType() should be(classOf[JsonProperty])
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation 2") {
    val fields = CaseClassField.createFields(
      classOf[FooBar],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)

    val helloField = fields.head
    helloField.name should equal("helloWorld")
    helloField.annotations.size should equal(2)
    helloField.annotations.head.annotationType() should be(classOf[JsonProperty])
    helloField.annotations.exists(_.annotationType() == classOf[Header]) should be(true)
    helloField.annotations.last.asInstanceOf[Header].value() should be("accept") // from Bar
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation 3") {
    val fields = CaseClassField.createFields(
      classOf[TestTraitImpl],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    /*
      in trait:
      ---------
      @JsonProperty("oldness")
      def age: Int
      @NotEmpty
      def name: String

      case class constructor:
      -----------------------
      @JsonProperty("ageness") age: Int, // should override inherited annotation from trait
      @Header name: String, // should have two annotations, one from trait and one here
      @QueryParam dateTime: DateTime,
      @JsonProperty foo: String, // empty JsonProperty should default to field name
      @JsonDeserialize(contentAs = classOf[BigDecimal], using = classOf[BigDecimalDeserializer])
      double: BigDecimal,
      @JsonIgnore ignoreMe: String
     */
    fields.length should equal(6)

    val fieldMap: Map[String, CaseClassField] =
      fields.map(field => field.name -> field).toMap

    val ageField = fieldMap("ageness")
    ageField.annotations.size should equal(1)
    ageField.annotations.exists(_.annotationType() == classOf[JsonProperty]) should be(true)
    ageField.annotations.head.asInstanceOf[JsonProperty].value() should equal("ageness")

    val nameField = fieldMap("name")
    nameField.annotations.size should equal(2)
    nameField.annotations.exists(_.annotationType() == classOf[NotEmpty]) should be(true)
    nameField.annotations.exists(_.annotationType() == classOf[Header]) should be(true)

    val dateTimeField = fieldMap("dateTime")
    dateTimeField.annotations.size should equal(1)
    dateTimeField.annotations.exists(_.annotationType() == classOf[QueryParam]) should be(true)

    val fooField = fieldMap("foo")
    fooField.annotations.size should equal(1)
    fooField.annotations.exists(_.annotationType() == classOf[JsonProperty]) should be(true)
    fooField.annotations.head.asInstanceOf[JsonProperty].value() should equal("")

    val doubleField = fieldMap("double")
    doubleField.annotations.size should equal(1)
    doubleField.annotations.exists(_.annotationType() == classOf[JsonDeserialize]) should be(true)
    doubleField.annotations.head.asInstanceOf[JsonDeserialize].contentAs() should be(
      classOf[BigDecimal])
    doubleField.annotations.head.asInstanceOf[JsonDeserialize].using() should be(
      classOf[BigDecimalDeserializer])
    doubleField.deserializer should not be None

    val ignoreMeField = fieldMap("ignoreMe")
    ignoreMeField.annotations.size should equal(1)
    ignoreMeField.annotations.exists(_.annotationType() == classOf[JsonIgnore]) should be(true)
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation 4") {
    val fields: Seq[CaseClassField] = CaseClassField.createFields(
      classOf[FooBaz],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    val helloField: CaseClassField = fields.head
    helloField.annotations.size should equal(2)
    helloField.annotations.exists(_.annotationType() == classOf[JsonProperty]) should be(true)
    helloField.annotations.head
      .asInstanceOf[JsonProperty].value() should equal("goodbyeWorld") // from Baz

    helloField.annotations.exists(_.annotationType() == classOf[Header]) should be(true)
    helloField.annotations.last.asInstanceOf[Header].value() should be("accept") // from Bar
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation 5") {
    val fields = CaseClassField.createFields(
      classOf[FooBarBaz],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    val helloField: CaseClassField = fields.head
    helloField.annotations.size should equal(2)
    helloField.annotations.exists(_.annotationType() == classOf[JsonProperty]) should be(true)
    helloField.annotations.head
      .asInstanceOf[JsonProperty].value() should equal("goodbye") // from BarBaz

    helloField.annotations.exists(_.annotationType() == classOf[Header]) should be(true)
    helloField.annotations.last.asInstanceOf[Header].value() should be("accept") // from Bar
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation 6") {
    val fields = CaseClassField.createFields(
      classOf[File],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    val uriField: CaseClassField = fields.head
    uriField.annotations.size should equal(1)
    uriField.annotations.exists(_.annotationType() == classOf[JsonProperty]) should be(true)
    uriField.annotations.head.asInstanceOf[JsonProperty].value() should equal("file")
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation 7") {
    val fields = CaseClassField.createFields(
      classOf[Folder],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    val uriField: CaseClassField = fields.head
    uriField.annotations.size should equal(1)
    uriField.annotations.exists(_.annotationType() == classOf[JsonProperty]) should be(true)
    uriField.annotations.head.asInstanceOf[JsonProperty].value() should equal("folder")
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation 8") {
    val fields = CaseClassField.createFields(
      classOf[LoadableFile],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    val uriField: CaseClassField = fields.head
    uriField.annotations.size should equal(1)
    uriField.annotations.exists(_.annotationType() == classOf[JsonProperty]) should be(true)
    uriField.annotations.head.asInstanceOf[JsonProperty].value() should equal("file")
  }

  test("CaseClassField.createFields sees inherited JsonProperty annotation 9") {
    val fields = CaseClassField.createFields(
      classOf[LoadableFolder],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    val uriField: CaseClassField = fields.head
    uriField.annotations.size should equal(1)
    uriField.annotations.exists(_.annotationType() == classOf[JsonProperty]) should be(true)
    uriField.annotations.head.asInstanceOf[JsonProperty].value() should equal("folder")
  }

  test("Seq[Long]") {
    val fields = CaseClassField.createFields(
      classOf[CaseClassWithArrayLong],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )

    fields.length should equal(1)
    val arrayField: CaseClassField = fields.head
    arrayField.javaType.getTypeName should be("[array type, component type: [simple type, class long]]")
  }

  test("CaseClassField should filter case class annotations correctly") {
    val fields = CaseClassField.createFields(
      classOf[TestTraitImpl],
      PropertyNamingStrategy.LOWER_CAMEL_CASE,
      TypeFactory.defaultInstance,
      NullCaseClassValidationProvider
    )
    val nameField = fields.filter(_.name == "name")
    val fieldAnnotations = nameField.flatMap(_.validationAnnotations)
    fieldAnnotations.head.annotationType() should be (classOf[NotEmptyInternal])
  }
}
