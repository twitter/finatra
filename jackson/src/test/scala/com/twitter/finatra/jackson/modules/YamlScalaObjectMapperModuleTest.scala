package com.twitter.finatra.jackson.modules

import com.fasterxml.jackson.databind.{
  DeserializationFeature,
  JsonMappingException,
  ObjectMapper => JacksonObjectMapper
}
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.twitter.finatra.jackson.{
  CaseClass,
  ClassWithFooClassInject,
  ClassWithQueryParamDateTimeInject,
  LooseCaseClass,
  SimpleClassWithInjection,
  StrictCaseClass
}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, Test}
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.jackson.caseclass.exceptions.{
  CaseClassMappingException,
  InjectableValuesException
}
import org.junit.Assert.{assertNotNull, assertTrue}

object YamlScalaObjectMapperModuleTest {
  val failOnUnknownPropertiesModule: ScalaObjectMapperModule = new YamlScalaObjectMapperModule {
    override protected def additionalMapperConfiguration(mapper: JacksonObjectMapper): Unit =
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
  }
}

class YamlScalaObjectMapperModuleTest extends Test {
  import YamlScalaObjectMapperModuleTest._

  private[this] val mapperModule: YamlScalaObjectMapperModule = YamlScalaObjectMapperModule

  /* Test Injector */
  private[this] final val injector: Injector = TestInjector(mapperModule).create()
  /* Class under test */
  private[this] val mapper: ScalaObjectMapper = injector.instance[ScalaObjectMapper]

  test("module constructors") {
    //jacksonScalaObjectMapper
    assertNotNull(mapperModule.jacksonScalaObjectMapper)
    assertTrue(mapperModule.jacksonScalaObjectMapper.getFactory.isInstanceOf[YAMLFactory])
    assertNotNull(YamlScalaObjectMapperModule.jacksonScalaObjectMapper)
    assertTrue(
      YamlScalaObjectMapperModule.jacksonScalaObjectMapper.getFactory.isInstanceOf[YAMLFactory])

    assertNotNull(mapperModule.jacksonScalaObjectMapper(injector.underlying))
    assertTrue(
      mapperModule
        .jacksonScalaObjectMapper(injector.underlying).getFactory.isInstanceOf[YAMLFactory])
    assertNotNull(YamlScalaObjectMapperModule.jacksonScalaObjectMapper(injector.underlying))
    assertTrue(
      YamlScalaObjectMapperModule
        .jacksonScalaObjectMapper(injector.underlying).getFactory.isInstanceOf[YAMLFactory])

    assertNotNull(mapperModule.jacksonScalaObjectMapper(null))
    assertTrue(
      mapperModule
        .jacksonScalaObjectMapper(null).getFactory.isInstanceOf[YAMLFactory])
    assertNotNull(YamlScalaObjectMapperModule.jacksonScalaObjectMapper(null))
    assertTrue(
      YamlScalaObjectMapperModule
        .jacksonScalaObjectMapper(null).getFactory.isInstanceOf[YAMLFactory])

    //objectMapper
    assertNotNull(mapperModule.objectMapper)
    assertTrue(mapperModule.objectMapper.underlying.getFactory.isInstanceOf[YAMLFactory])
    assertNotNull(YamlScalaObjectMapperModule.objectMapper)
    assertTrue(mapperModule.objectMapper.underlying.getFactory.isInstanceOf[YAMLFactory])

    assertNotNull(mapperModule.objectMapper(injector.underlying))
    assertTrue(
      mapperModule
        .objectMapper(injector.underlying).underlying.getFactory.isInstanceOf[YAMLFactory])
    assertNotNull(YamlScalaObjectMapperModule.objectMapper(injector.underlying))
    assertTrue(
      YamlScalaObjectMapperModule
        .objectMapper(injector.underlying).underlying.getFactory.isInstanceOf[YAMLFactory])

    assertNotNull(mapperModule.objectMapper(null))
    assertTrue(mapperModule.objectMapper(null).underlying.getFactory.isInstanceOf[YAMLFactory])
    assertNotNull(YamlScalaObjectMapperModule.objectMapper(null))
    assertTrue(
      YamlScalaObjectMapperModule
        .objectMapper(null).underlying.getFactory.isInstanceOf[YAMLFactory])

    // mappers for property naming strategies
    assertNotNull(mapperModule.camelCaseObjectMapper)
    assertTrue(mapperModule.camelCaseObjectMapper.underlying.getFactory.isInstanceOf[YAMLFactory])
    assertNotNull(YamlScalaObjectMapperModule.camelCaseObjectMapper)
    assertTrue(
      YamlScalaObjectMapperModule.camelCaseObjectMapper.underlying.getFactory
        .isInstanceOf[YAMLFactory])

    assertNotNull(mapperModule.snakeCaseObjectMapper)
    assertTrue(mapperModule.snakeCaseObjectMapper.underlying.getFactory.isInstanceOf[YAMLFactory])
    assertNotNull(YamlScalaObjectMapperModule.snakeCaseObjectMapper)
    assertTrue(
      YamlScalaObjectMapperModule.snakeCaseObjectMapper.underlying.getFactory
        .isInstanceOf[YAMLFactory])
  }

  test("inject an un-constructable field fails with an injectable values exception") {
    // when there is an injector it tries to find FooClass from injector but the injector
    // can't construct a FooClass, thus an injectable values exception.
    intercept[InjectableValuesException] {
      parse[ClassWithFooClassInject]("""---
          |fooClass: shouldBeInjected
          |""".stripMargin)
    }
  }

  test("mapper from injection handles unknown properties") {
    val testInjector = TestInjector(failOnUnknownPropertiesModule).create()
    val testMapper = testInjector.instance[ScalaObjectMapper]

    // mapper = strict, case class = unannotated --> Fail
    intercept[JsonMappingException] {
      testMapper.parse[CaseClass](
        """---
          |id: 12345
          |name: gadget
          |extra: fail""".stripMargin
      )
    }

    // mapper = strict, case class = annotated strict --> Fail
    intercept[JsonMappingException] {
      testMapper.parse[StrictCaseClass](
        """---
          |id: 12345
          |name: gadget
          |extra: fail""".stripMargin
      )
    }

    // mapper = strict, case class = annotated loose --> Parse
    testMapper.parse[LooseCaseClass](
      """---
        |id: 12345
        |name: gadget
        |extra: pass""".stripMargin
    )
  }

  test("deserialization#inject request field fails with mapping exception") {
    // with an injector and GuiceInjectableValues the field ends up with
    // a new instance constructed for Joda DateTime by the injector
    // USERS BEWARE -- if the GuiceInjectableValues is configured, you can get a default instance as
    // an injected value if you annotate the field accordingly
    intercept[CaseClassMappingException] {
      parse[ClassWithQueryParamDateTimeInject]("""---
          |dateTime: shouldBeInjected
          |""".stripMargin)
    }
  }

  test("deserialization#parse SimpleClassWithInjection fails") {
    // with an Injector: SimpleClassWithInjection has a @FooInjectableValue annotation for which
    // there is no Jackson InjectableValues which supports the annotation, however since we
    // provide the field in JSON that value is returned.
    val yaml =
      """---
      |hello: Mom
      |""".stripMargin

    parse[SimpleClassWithInjection](yaml) should equal(SimpleClassWithInjection("Mom"))
  }

  protected def parse[T: Manifest](string: String): T = {
    mapper.parse[T](string)
  }
}
