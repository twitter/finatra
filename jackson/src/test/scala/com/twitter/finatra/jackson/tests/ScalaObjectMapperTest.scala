package com.twitter.finatra.jackson.tests

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonMappingException}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.caseclass.exceptions.CaseClassMappingException
import com.twitter.finatra.jackson.tests.AbstractScalaObjectMapperTest.ZeroOrOneDeserializer

class ScalaObjectMapperTest extends AbstractScalaObjectMapperTest {
  /* Class under test */
  override protected val mapper: ScalaObjectMapper = ScalaObjectMapper()

  test("constructors") {
    assert(ScalaObjectMapper() != null)
    assert(ScalaObjectMapper.apply() != null)

    assert(ScalaObjectMapper(injector = null) != null)
    assert(ScalaObjectMapper.apply(injector = null) != null)

    assertThrows[AssertionError](new ScalaObjectMapper(null))
  }

  test("mapper register module") {
    val testMapper = ScalaObjectMapper()

    val simpleJacksonModule = new SimpleModule()
    simpleJacksonModule.addDeserializer(classOf[ZeroOrOne], new ZeroOrOneDeserializer)
    testMapper.registerModule(simpleJacksonModule)

    // regular mapper (without the custom deserializer) -- doesn't parse
    intercept[JsonMappingException] {
      mapper.parse[CaseClassWithZeroOrOne]("{\"id\" :\"zero\"}")
    }

    // mapper with custom deserializer -- parses correctly
    testMapper.parse[CaseClassWithZeroOrOne]("{\"id\" :\"zero\"}") should be(
      CaseClassWithZeroOrOne(Zero))
    testMapper.parse[CaseClassWithZeroOrOne]("{\"id\" :\"one\"}") should be(
      CaseClassWithZeroOrOne(One))
    intercept[JsonMappingException] {
      testMapper.parse[CaseClassWithZeroOrOne]("{\"id\" :\"two\"}")
    }
  }

  test("inject request field fails with a mapping exception") {
    // with no injector we get a CaseClassMappingException since we can't map
    // the JSON into the case class.
    intercept[CaseClassMappingException] {
      parse[ClassWithQueryParamDateTimeInject]("""{}""")
    }
  }

  test(
    "class with an injectable field fails with a mapping exception when it cannot be parsed from JSON") {
    // if there is no injector, the default injectable values is not configured, thus the code
    // tries to construct the asked for type from the given JSON, which is empty and fails with
    // a mapping exception.
    intercept[CaseClassMappingException] {
      parse[ClassWithFooClassInject]("""{}""")
    }
  }

  test("class with an injectable field is not constructed from JSON") {
    // if there is no injector then the default injectable values is not configured,
    // thus the code cannot satisfy the required field and a mapping exception is returned.
    intercept[CaseClassMappingException] {
      parse[ClassWithFooClassInject](
        """
          |{
          |  "foo_class": {
          |    "id" : "1"
          |  }
          |}""".stripMargin
      )
    }
  }

  test("class with a defaulted injectable field is constructed from the default") {
    // yes this is confusing: an inject annotated field but with no injector yet there's a
    // default, thus you get the default not an exception.
    val result = parse[ClassWithFooClassInjectAndDefault](
      """
        |{
        |  "foo_class": {
        |    "id" : "1"
        |  }
        |}""".stripMargin
    )

    result.fooClass should equal(FooClass("12345"))
  }

  test("regular mapper handles unknown properties") {
    // regular mapper -- doesn't fail
    mapper.parse[CaseClass](
      """
        |{
        |  "id": 12345,
        |  "name": "gadget",
        |  "extra": "fail"
        |}
        |""".stripMargin
    )

    // mapper = loose, case class = annotated strict --> Fail
    intercept[JsonMappingException] {
      mapper.parse[StrictCaseClass](
        """
          |{
          |  "id": 12345,
          |  "name": "gadget",
          |  "extra": "fail"
          |}
          |""".stripMargin
      )
    }
  }

  test("mapper with deserialization config fails on unknown properties") {
    val testMapper =
      ScalaObjectMapper.builder
        .withDeserializationConfig(Map(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES -> true))
        .objectMapper

    // mapper = strict, case class = unannotated --> Fail
    intercept[JsonMappingException] {
      testMapper.parse[CaseClass](
        """
          |{
          |  "id": 12345,
          |  "name": "gadget",
          |  "extra": "fail"
          |}
          |""".stripMargin
      )
    }

    // mapper = strict, case class = annotated strict --> Fail
    intercept[JsonMappingException] {
      testMapper.parse[StrictCaseClass](
        """
          |{
          |  "id": 12345,
          |  "name": "gadget",
          |  "extra": "fail"
          |}
          |""".stripMargin
      )
    }

    // mapper = strict, case class = annotated loose --> Parse
    testMapper.parse[LooseCaseClass](
      """
        |{
        |  "id": 12345,
        |  "name": "gadget",
        |  "extra": "pass"
        |}
        |""".stripMargin
    )
  }

  test("mapper with additional configuration handles unknown properties") {
    // test with additional configuration set on mapper
    val testMapper =
      ScalaObjectMapper.builder
        .withAdditionalMapperConfigurationFn(
          _.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true))
        .objectMapper

    // mapper = strict, case class = unannotated --> Fail
    intercept[JsonMappingException] {
      testMapper.parse[CaseClass](
        """
          |{
          |  "id": 12345,
          |  "name": "gadget",
          |  "extra": "fail"
          |}
          |""".stripMargin
      )
    }

    // mapper = strict, case class = annotated strict --> Fail
    intercept[JsonMappingException] {
      testMapper.parse[StrictCaseClass](
        """
          |{
          |  "id": 12345,
          |  "name": "gadget",
          |  "extra": "fail"
          |}
          |""".stripMargin
      )
    }

    // mapper = strict, case class = annotated loose --> Parse
    testMapper.parse[LooseCaseClass](
      """
        |{
        |  "id": 12345,
        |  "name": "gadget",
        |  "extra": "pass"
        |}
        |""".stripMargin
    )
  }

  test("support camel case mapper") {
    val camelCaseObjectMapper = ScalaObjectMapper.camelCaseObjectMapper(mapper.underlying)

    camelCaseObjectMapper.parse[Map[String, String]]("""{"firstName": "Bob"}""") should equal(
      Map("firstName" -> "Bob")
    )
  }

  test("support snake case mapper") {
    val snakeCaseObjectMapper = ScalaObjectMapper.snakeCaseObjectMapper(mapper.underlying)

    val person = CamelCaseSimplePersonNoAnnotation(myName = "Bob")

    val serialized = snakeCaseObjectMapper.writeValueAsString(person)
    serialized should equal("""{"my_name":"Bob"}""")
    snakeCaseObjectMapper.parse[CamelCaseSimplePersonNoAnnotation](serialized) should equal(person)
  }
}
