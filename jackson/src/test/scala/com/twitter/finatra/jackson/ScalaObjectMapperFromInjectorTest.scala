package com.twitter.finatra.jackson

import com.fasterxml.jackson.databind.JsonMappingException
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.caseclass.exceptions.InjectableValuesException
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.finatra.jackson.AbstractScalaObjectMapperTest.failOnUnknownPropertiesModule
import com.twitter.finatra.json.annotations.{CamelCaseMapper, SnakeCaseMapper}
import com.twitter.inject.Injector
import com.twitter.inject.app.TestInjector

class ScalaObjectMapperFromInjectorTest extends AbstractScalaObjectMapperTest {
  private[this] val mapperModule: ScalaObjectMapperModule = ScalaObjectMapperModule

  /* Test Injector */
  private[this] final val injector: Injector = TestInjector(mapperModule).create
  /* Class under test */
  override protected val mapper: ScalaObjectMapper = injector.instance[ScalaObjectMapper]

  test("constructors") {
    assert(ScalaObjectMapper(injector.underlying) != null)
    assert(ScalaObjectMapper.apply(injector.underlying) != null)

    assert(ScalaObjectMapper(copy(mapper.underlying)) != null)
    assert(ScalaObjectMapper.apply(copy(mapper.underlying)) != null)

    assert(ScalaObjectMapper(null, copy(mapper.underlying)) != null)
    assert(ScalaObjectMapper.apply(null, copy(mapper.underlying)) != null)

    assert(ScalaObjectMapper(injector.underlying, copy(mapper.underlying)) != null)
    assert(ScalaObjectMapper.apply(injector.underlying, copy(mapper.underlying)) != null)

    assertThrows[AssertionError](new ScalaObjectMapper(null))
  }

  test("module constructors") {
    assert(mapperModule.jacksonScalaObjectMapper != null)
    assert(ScalaObjectMapperModule.jacksonScalaObjectMapper != null)

    assert(mapperModule.jacksonScalaObjectMapper(injector.underlying) != null)
    assert(ScalaObjectMapperModule.jacksonScalaObjectMapper(injector.underlying) != null)

    assert(mapperModule.jacksonScalaObjectMapper(null) != null)
    assert(ScalaObjectMapperModule.jacksonScalaObjectMapper(null) != null)

    assert(
      mapperModule.jacksonScalaObjectMapper(injector.underlying, copy(mapper.underlying)) != null)
    assert(
      ScalaObjectMapperModule
        .jacksonScalaObjectMapper(injector.underlying, copy(mapper.underlying)) != null)

    assert(mapperModule.jacksonScalaObjectMapper(null, copy(mapper.underlying)) != null)
    assert(ScalaObjectMapperModule.jacksonScalaObjectMapper(null, copy(mapper.underlying)) != null)

    assert(mapperModule.objectMapper != null)
    assert(ScalaObjectMapperModule.objectMapper != null)

    assert(mapperModule.objectMapper(injector.underlying) != null)
    assert(ScalaObjectMapperModule.objectMapper(injector.underlying) != null)

    assert(mapperModule.objectMapper(null) != null)
    assert(ScalaObjectMapperModule.objectMapper(null) != null)

    assert(mapperModule.camelCaseObjectMapper != null)
    assert(ScalaObjectMapperModule.camelCaseObjectMapper != null)

    assert(mapperModule.snakeCaseObjectMapper != null)
    assert(ScalaObjectMapperModule.snakeCaseObjectMapper != null)
  }

  test("inject an un-constructable field fails with an injectable values exception") {
    // when there is an injector it tries to find FooClass from injector but the injector
    // can't construct a FooClass, thus an injectable values exception.
    intercept[InjectableValuesException] {
      parse[ClassWithFooClassInject]("""{}""")
    }
  }

  test("mapper from injection handles unknown properties") {
    val testInjector = TestInjector(failOnUnknownPropertiesModule).create
    val testMapper = testInjector.instance[ScalaObjectMapper]

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

  test("support camel case mapper#camel case object") {
    val camelCaseObjectMapper = injector.instance[ScalaObjectMapper, CamelCaseMapper]

    camelCaseObjectMapper.parse[Map[String, String]]("""{"firstName": "Bob"}""") should equal(
      Map("firstName" -> "Bob")
    )
  }

  test("support snake case mapper#snake case object") {
    val snakeCaseObjectMapper = injector.instance[ScalaObjectMapper, SnakeCaseMapper]

    val person = CamelCaseSimplePersonNoAnnotation(myName = "Bob")

    val serialized = snakeCaseObjectMapper.writeValueAsString(person)
    serialized should equal("""{"my_name":"Bob"}""")
    snakeCaseObjectMapper.parse[CamelCaseSimplePersonNoAnnotation](serialized) should equal(person)
  }
}
