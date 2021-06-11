package com.twitter.finatra.jackson.modules

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
import com.fasterxml.jackson.databind.{
  Module => JacksonModule,
  ObjectMapper => JacksonObjectMapper,
  _
}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.google.inject.{Injector, Provides}
import com.twitter.finatra.jackson.caseclass.{DefaultAnnotationIntrospector, GuiceInjectableValues}
import com.twitter.finatra.jackson.serde.BaseSerdeModule
import com.twitter.finatra.json.annotations.{CamelCaseMapper, SnakeCaseMapper}
import com.twitter.inject.TwitterModule
import com.twitter.util.jackson.{JacksonScalaObjectMapperType, ScalaObjectMapper}
import com.twitter.util.validation.ScalaValidator
import javax.annotation.Nullable
import javax.inject.Singleton

object ScalaObjectMapperModule extends ScalaObjectMapperModule {
  // java-friendly access to singleton
  def get(): this.type = this
}

/**
 * [[TwitterModule]] to configure Jackson object mappers. Extend this module to override defaults
 * or provide additional configuration to the bound [[ScalaObjectMapper]] instances.
 *
 * Example:
 *
 * {{{
 *    import com.fasterxml.jackson.databind.{
 *      DeserializationFeature,
 *      Module,
 *      ObjectMapper,
 *      PropertyNamingStrategy
 *    }
 *    import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
 *
 *    object MyCustomObjectMapperModule extends ScalaObjectMapperModule {
 *
 *      override val propertyNamingStrategy: PropertyNamingStrategy =
 *        new PropertyNamingStrategy.KebabCaseStrategy
 *
 *      override val additionalJacksonModules: Seq[Module] =
 *        Seq(MySimpleJacksonModule)
 *
 *      override def additionalMapperConfiguration(mapper: ObjectMapper): Unit = {
 *        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
 *       }
 *     }
 * }}}
 */
class ScalaObjectMapperModule extends TwitterModule {

  /* Note: this is a stateful Module */
  private[this] val builder = ScalaObjectMapper.builder

  override protected def configure(): Unit = {
    bindOption[ScalaValidator]
  }

  /* Public */

  /** Return a [[JacksonScalaObjectMapperType]] configured from this [[ScalaObjectMapperModule]]. */
  def jacksonScalaObjectMapper: JacksonScalaObjectMapperType =
    withBuilder.objectMapper.underlying

  /** Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]]. */
  def objectMapper: ScalaObjectMapper = withBuilder.objectMapper

  /**
   * Return a [[JacksonScalaObjectMapperType]] configured from this [[ScalaObjectMapperModule]]
   * using the given (nullable) [[Injector]].
   *
   * @param injector a configured (nullable) [[Injector]].
   */
  def jacksonScalaObjectMapper(@Nullable injector: Injector): JacksonScalaObjectMapperType = {
    val withGuiceInjectableValues = configureGuiceInjectableValues(injector)
    withBuilder
      .withAdditionalMapperConfigurationFn(withGuiceInjectableValues)
      .objectMapper
      .underlying
  }

  /**
   * Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]]
   * using the given (nullable) [[Injector]].
   *
   * @param injector a configured (nullable) [[Injector]].
   */
  def objectMapper(@Nullable injector: Injector): ScalaObjectMapper = {
    val withGuiceInjectableValues = configureGuiceInjectableValues(injector)
    withBuilder
      .withAdditionalMapperConfigurationFn(withGuiceInjectableValues)
      .objectMapper
  }

  /**
   * Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]] explicitly
   * configured with [[PropertyNamingStrategy.LOWER_CAMEL_CASE]] as a `PropertyNamingStrategy`.
   */
  final def camelCaseObjectMapper: ScalaObjectMapper =
    ScalaObjectMapper.camelCaseObjectMapper(this.jacksonScalaObjectMapper(null))

  /**
   * Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]] explicitly
   * configured with [[PropertyNamingStrategy.SNAKE_CASE]] as a `PropertyNamingStrategy`.
   */
  final def snakeCaseObjectMapper: ScalaObjectMapper =
    ScalaObjectMapper.snakeCaseObjectMapper(this.jacksonScalaObjectMapper(null))

  /* Protected -- users are expected to customize behavior by overriding these methods and members */

  /**
   * @see [[ScalaObjectMapper.DefaultSerializationInclude]]
   * @see [[ScalaObjectMapper.Builder.withSerializationInclude]]
   */
  protected def serializationInclusion: Include = ScalaObjectMapper.DefaultSerializationInclude

  /**
   * @see [[ScalaObjectMapper.DefaultSerializationConfig]]
   * @see [[ScalaObjectMapper.Builder.withSerializationInclude]]
   */
  protected def serializationConfig: Map[SerializationFeature, Boolean] =
    ScalaObjectMapper.DefaultSerializationConfig

  /**
   * @see [[ScalaObjectMapper.DefaultDeserializationConfig]]
   * @see [[ScalaObjectMapper.Builder.withDeserializationConfig]]
   */
  protected def deserializationConfig: Map[DeserializationFeature, Boolean] =
    ScalaObjectMapper.DefaultDeserializationConfig

  /**
   * @see [[ScalaObjectMapper.DefaultPropertyNamingStrategy]]
   * @see [[ScalaObjectMapper.Builder.withPropertyNamingStrategy]]
   */
  protected def propertyNamingStrategy: PropertyNamingStrategy =
    ScalaObjectMapper.DefaultPropertyNamingStrategy

  /**
   * @see [[ScalaObjectMapper.DefaultNumbersAsStrings]]
   * @see [[ScalaObjectMapper.Builder.withNumbersAsStrings]]
   * @see [[com.fasterxml.jackson.core.json.JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS]]
   */
  protected def numbersAsStrings: Boolean = ScalaObjectMapper.DefaultNumbersAsStrings

  /**
   * @see [[ScalaObjectMapper.DefaultJacksonModules]]
   * @see [[ScalaObjectMapper.Builder.withDefaultJacksonModules]]
   */
  protected def defaultJacksonModules: Seq[JacksonModule] =
    ScalaObjectMapper.DefaultJacksonModules ++ Seq(new JodaModule, BaseSerdeModule)

  /**
   * @see [[ScalaObjectMapper.DefaultAdditionalJacksonModules]]
   * @see [[ScalaObjectMapper.Builder.withAdditionalJacksonModules]]
   */
  protected def additionalJacksonModules: Seq[JacksonModule] =
    ScalaObjectMapper.DefaultAdditionalJacksonModules

  /**
   * @see [[ScalaObjectMapper.Builder.withAdditionalMapperConfigurationFn]]
   * @param mapper the underlying [[JacksonObjectMapper]] to configure
   * @note caution -- it is expected that this method mutates the given mapper reference.
   */
  protected def additionalMapperConfiguration(mapper: JacksonObjectMapper): Unit = {}

  /**
   * @see [[ScalaObjectMapper.DefaultValidation]]
   * @see [[ScalaObjectMapper.Builder.withNoValidation]]
   */
  protected def validation: Boolean = ScalaObjectMapper.DefaultValidation

  /** Convenience method for copying a [[JacksonScalaObjectMapperType]]  */
  protected final def copy(
    objectMapper: JacksonScalaObjectMapperType
  ): JacksonScalaObjectMapperType =
    ObjectMapperCopier.copy(objectMapper)

  // allows for overriding the injectable types via the provides method for injection
  protected[modules] def provideScalaObjectMapper(
    injector: Injector,
    validator: Option[ScalaValidator]
  ): ScalaObjectMapper =
    provideConfiguredObjectMapperBuilder(injector, validator).objectMapper

  /* Private */

  private[modules] final def provideConfiguredObjectMapperBuilder(
    injector: Injector,
    validator: Option[ScalaValidator]
  ): ScalaObjectMapper.Builder = {
    val withGuiceInjectableValues = configureGuiceInjectableValues(injector)

    val builderWithValidator = validator match {
      case Some(v) => withBuilder.withValidator(v)
      case _ => withBuilder
    }

    builderWithValidator
      .withAdditionalMapperConfigurationFn(withGuiceInjectableValues)
  }

  private[modules] final def withBuilder: ScalaObjectMapper.Builder = {
    val base = builder
      .withPropertyNamingStrategy(this.propertyNamingStrategy)
      .withNumbersAsStrings(this.numbersAsStrings)
      .withSerializationInclude(this.serializationInclusion)
      .withSerializationConfig(this.serializationConfig)
      .withDeserializationConfig(this.deserializationConfig)
      .withDefaultJacksonModules(this.defaultJacksonModules)
      .withAdditionalJacksonModules(this.additionalJacksonModules)
      .withAdditionalMapperConfigurationFn(this.additionalMapperConfiguration)
    if (validation) base else base.withNoValidation
  }

  private[modules] final def configureGuiceInjectableValues(
    injector: Injector
  ): JacksonObjectMapper => Unit = { mapper =>
    val defaultAnnotationIntrospector: DefaultAnnotationIntrospector =
      new DefaultAnnotationIntrospector
    mapper.setInjectableValues(new GuiceInjectableValues(injector))
    mapper.setAnnotationIntrospectors(
      new AnnotationIntrospectorPair(
        defaultAnnotationIntrospector,
        mapper.getSerializationConfig.getAnnotationIntrospector
      ),
      new AnnotationIntrospectorPair(
        defaultAnnotationIntrospector,
        mapper.getDeserializationConfig.getAnnotationIntrospector
      )
    )
  }

  /* NOTE: these methods are private as they are only meant to be called by the
     injector for providing these types. */

  // We explicitly add the `validator: Option[ScalaValidator]` as a parameter
  // to force the injector to resolve this type instead of attempting to read it from
  // the passed injector which may not be completely initialized here.
  @Singleton
  @Provides
  @SnakeCaseMapper
  private final def provideSnakeCaseObjectMapper(
    injector: Injector,
    validator: Option[ScalaValidator]
  ): ScalaObjectMapper =
    ScalaObjectMapper.snakeCaseObjectMapper(
      provideScalaObjectMapper(injector, validator).underlying
    )

  @Singleton
  @Provides
  @CamelCaseMapper
  private final def provideCamelCaseObjectMapper(
    injector: Injector,
    validator: Option[ScalaValidator]
  ): ScalaObjectMapper =
    ScalaObjectMapper.camelCaseObjectMapper(
      provideScalaObjectMapper(injector, validator).underlying
    )

  @Singleton
  @Provides
  private final def provideObjectMapper(
    injector: Injector,
    validator: Option[ScalaValidator]
  ): ScalaObjectMapper =
    provideScalaObjectMapper(injector, validator)
}
