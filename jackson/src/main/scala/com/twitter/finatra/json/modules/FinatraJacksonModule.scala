package com.twitter.finatra.json.modules

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonGenerator.Feature
import com.fasterxml.jackson.databind.{Module => JacksonModule, _}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.inject.{Injector, Provides}
import com.twitter.finatra.annotations.CamelCaseMapper
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.guice.GuiceInjectableValues
import com.twitter.finatra.json.internal.caseclass.jackson.FinatraCaseClassModule
import com.twitter.finatra.json.internal.serde.{FinatraSerDeSimpleModule, LongKeyDeserializers}
import com.twitter.finatra.json.utils.CamelCasePropertyNamingStrategy
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

import scala.collection.JavaConverters._


object FinatraJacksonModule extends FinatraJacksonModule

/**
 * Guice module to configure Jackson object mappers
 *
 * Note: Simply extend this module to override defaults or provide
 * additional configuration. See SampleApiServer for an example.
 */
class FinatraJacksonModule extends TwitterModule {

  /* Public */

  /* Note: We avoid creating FinatraObjectMapper w/ an @Inject annotation and instead
     prefer explicit building here. This protects us from getting a default
     no-arg Jackson ObjectMapper injected into FinatraObjectMapper */
  @Singleton
  @Provides
  def provideFinatraObjectMapper(
    objectMapper: ObjectMapper with ScalaObjectMapper): FinatraObjectMapper = {
    new FinatraObjectMapper(objectMapper)
  }

  /*
   * Create a FinatraObjectMapper annotated w/ @CamelCaseMapper that always uses camelCase.
   * This is useful when you need to mix different naming strategies in the same application
   * (e.g. serialize your API w/ snake_case but use this mapper to parse remote services w/ camelCase
   */
  @Singleton
  @Provides
  @CamelCaseMapper
  def provideCamelCaseFinatraObjectMapper(objectMapper: ObjectMapper with ScalaObjectMapper): FinatraObjectMapper = {
    val objectMapperCopy = copy(objectMapper)
    objectMapperCopy.setPropertyNamingStrategy(CamelCasePropertyNamingStrategy)
    new FinatraObjectMapper(objectMapperCopy)
  }

  @Singleton
  @Provides
  def provideScalaObjectMapper(injector: Injector): ObjectMapper with ScalaObjectMapper = {
    val mapper = new ObjectMapper with ScalaObjectMapper

    defaultMapperConfiguration(mapper)
    additionalMapperConfiguration(mapper)

    mapper.setPropertyNamingStrategy(propertyNamingStrategy)
    mapper.registerModules(defaultJacksonModules.asJava)
    finatraCaseClassModule foreach mapper.registerModule
    mapper.registerModules(additionalJacksonModules.asJava)

    if (numbersAsStrings) {
      mapper.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true)
    }

    if (injector != null) {
      mapper.setInjectableValues(new GuiceInjectableValues(injector))
    }

    mapper
  }

  /* Protected */

  /** Jackson Modules to load */
  protected def defaultJacksonModules: Seq[JacksonModule] = Seq(
    new JodaModule,
    new JavaTimeModule,
    DefaultScalaModule,
    LongKeyDeserializers,
    FinatraSerDeSimpleModule) //FinatraModule's need to be added 'last' so they can override existing deser's

  protected def finatraCaseClassModule: Option[JacksonModule] = {
    Some(FinatraCaseClassModule)
  }

  protected def numbersAsStrings: Boolean = false

  protected def defaultMapperConfiguration(mapper: ObjectMapper) {
    /* Serialization Config */
    mapper.setSerializationInclusion(serializationInclusion)
    for ((feature, state) <- serializationConfig) {
      mapper.configure(feature, state)
    }

    /* Deserialization Config */
    for ((feature, state) <- deserializationConfig) {
      mapper.configure(feature, state)
    }
  }

  protected val serializationInclusion: Include =
    JsonInclude.Include.NON_ABSENT

  protected val serializationConfig: Map[SerializationFeature, Boolean] = Map(
    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS -> false,
    SerializationFeature.WRITE_ENUMS_USING_TO_STRING -> true)

  protected val deserializationConfig: Map[DeserializationFeature, Boolean] = Map(
    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES -> true,
    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES -> false,
    DeserializationFeature.READ_ENUMS_USING_TO_STRING -> true,
    DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY -> true,
    DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY -> true /* see jackson-module-scala/issues/148 */)

  protected val propertyNamingStrategy: PropertyNamingStrategy = {
    new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy
  }

  protected def additionalJacksonModules: Seq[JacksonModule] = Seq()

  protected def additionalMapperConfiguration(mapper: ObjectMapper) {
  }

  protected def copy(objectMapper: ObjectMapper with ScalaObjectMapper) = {
    ObjectMapperCopier.copy(objectMapper)
  }
}
