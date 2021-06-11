package com.twitter.finatra.jackson.modules

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{
  DeserializationContext,
  DeserializationFeature,
  JsonDeserializer,
  JsonMappingException,
  ObjectMapperCopier,
  ObjectMapper => JacksonObjectMapper
}
import com.twitter.finatra.jackson._
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, Test}
import com.twitter.util.jackson.caseclass.exceptions.{
  CaseClassMappingException,
  InjectableValuesException
}
import com.twitter.util.jackson.{JacksonScalaObjectMapperType, ScalaObjectMapper}
import org.junit.Assert.assertNotNull

private object ScalaObjectMapperModuleTest {
  class ZeroOrOneDeserializer extends JsonDeserializer[ZeroOrOne] {
    override def deserialize(
      jsonParser: JsonParser,
      deserializationContext: DeserializationContext
    ): ZeroOrOne = {
      jsonParser.getValueAsString match {
        case "zero" => Zero
        case "one" => One
        case _ =>
          throw new JsonMappingException(null, "Invalid value")
      }
    }
  }

  val failOnUnknownPropertiesModule: ScalaObjectMapperModule = new ScalaObjectMapperModule {
    override protected def additionalMapperConfiguration(mapper: JacksonObjectMapper): Unit =
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
  }
}

class ScalaObjectMapperModuleTest extends Test {
  import ScalaObjectMapperModuleTest._

  private[this] val mapperModule: ScalaObjectMapperModule = ScalaObjectMapperModule

  /* Test Injector */
  private[this] final val injector: Injector = TestInjector(mapperModule).create()
  /* Class under test */
  private[this] val mapper: ScalaObjectMapper = injector.instance[ScalaObjectMapper]

  test("module constructors") {
    //jacksonScalaObjectMapper
    assertNotNull(mapperModule.jacksonScalaObjectMapper)
    assertNotNull(ScalaObjectMapperModule.jacksonScalaObjectMapper)

    assertNotNull(mapperModule.jacksonScalaObjectMapper(injector.underlying))
    assertNotNull(ScalaObjectMapperModule.jacksonScalaObjectMapper(injector.underlying))

    assertNotNull(mapperModule.jacksonScalaObjectMapper(null))
    assertNotNull(ScalaObjectMapperModule.jacksonScalaObjectMapper(null))

    //objectMapper
    assertNotNull(mapperModule.objectMapper)
    assertNotNull(ScalaObjectMapperModule.objectMapper)

    assertNotNull(mapperModule.objectMapper(injector.underlying))
    assertNotNull(ScalaObjectMapperModule.objectMapper(injector.underlying))

    assertNotNull(mapperModule.objectMapper(null))
    assertNotNull(ScalaObjectMapperModule.objectMapper(null))

    // mappers for property naming strategies
    assertNotNull(mapperModule.camelCaseObjectMapper)
    assertNotNull(ScalaObjectMapperModule.camelCaseObjectMapper)

    assertNotNull(mapperModule.snakeCaseObjectMapper)
    assertNotNull(ScalaObjectMapperModule.snakeCaseObjectMapper)
  }

  test("inject an un-constructable field fails with an injectable values exception") {
    // when there is an injector it tries to find FooClass from injector but the injector
    // can't construct a FooClass, thus an injectable values exception.
    intercept[InjectableValuesException] {
      parse[ClassWithFooClassInject]("""{}""")
    }
  }

  test("mapper from injection handles unknown properties") {
    val testInjector = TestInjector(failOnUnknownPropertiesModule).create()
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

  test("deserialization#inject request field fails with mapping exception") {
    // with an injector and GuiceInjectableValues the field ends up with
    // a new instance constructed for Joda DateTime by the injector
    // USERS BEWARE -- if the GuiceInjectableValues is configured, you can get a default instance as
    // an injected value if you annotate the field accordingly
    intercept[CaseClassMappingException] {
      parse[ClassWithQueryParamDateTimeInject]("""{}""")
    }
  }

  test("deserialization#parse SimpleClassWithInjection fails") {
    // with an Injector: SimpleClassWithInjection has a @FooInjectableValue annotation for which
    // there is no Jackson InjectableValues which supports the annotation, however since we
    // provide the field in JSON that value is returned.
    val json =
      """{
      |  "hello" : "Mom"
      |}""".stripMargin

    parse[SimpleClassWithInjection](json) should equal(SimpleClassWithInjection("Mom"))
  }

  protected def copy(underlying: JacksonScalaObjectMapperType): JacksonScalaObjectMapperType = {
    ObjectMapperCopier.copy(underlying)
  }

  protected def parse[T: Manifest](string: String): T = {
    mapper.parse[T](string)
  }
}
