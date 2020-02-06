package com.twitter.finatra.jackson.modules

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{
  Module => JacksonModule,
  ObjectMapper => JacksonObjectMapper,
  _
}
import com.google.inject.{Injector, Provides}
import com.twitter.finatra.jackson.caseclass.{InjectableTypes, NullInjectableTypes}
import com.twitter.finatra.jackson.{JacksonScalaObjectMapperType, ScalaObjectMapper}
import com.twitter.finatra.json.annotations.{CamelCaseMapper, SnakeCaseMapper}
import com.twitter.finatra.validation.ValidationProvider
import com.twitter.inject.TwitterModule
import javax.annotation.Nullable
import javax.inject.Singleton

object ScalaObjectMapperModule extends ScalaObjectMapperModule

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
    // allows injection of `Option[InjectableTypes]` which will be a None by default as
    // we do not provide any binding here.
    bindOption[InjectableTypes]
  }

  /* Public */

  /** Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]]. */
  final def objectMapper: ScalaObjectMapper =
    withBuilder.objectMapper

  /**
   * Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]]
   * using the given (nullable) [[Injector]].
   *
   * @param injector a configured (nullable) [[Injector]].
   */
  final def objectMapper(@Nullable injector: Injector): ScalaObjectMapper =
    withBuilder.objectMapper(injector)

  /**
   * Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]] explicitly
   * configured with [[PropertyNamingStrategy.LOWER_CAMEL_CASE]] as a `PropertyNamingStrategy`.
   */
  final def camelCaseObjectMapper: ScalaObjectMapper =
    withBuilder.camelCaseObjectMapper

  /**
   * Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]] explicitly
   * configured with [[PropertyNamingStrategy.SNAKE_CASE]] as a `PropertyNamingStrategy`.
   */
  final def snakeCaseObjectMapper: ScalaObjectMapper =
    withBuilder.snakeCaseObjectMapper

  /** Return a [[JacksonScalaObjectMapperType]] configured from this [[ScalaObjectMapperModule]]. */
  final def jacksonScalaObjectMapper: JacksonScalaObjectMapperType =
    withBuilder.jacksonScalaObjectMapper

  /**
   * Return a [[JacksonScalaObjectMapperType]] configured from this [[ScalaObjectMapperModule]]
   * using the given (nullable) [[Injector]].
   *
   * @param injector a configured (nullable) [[Injector]].
   */
  final def jacksonScalaObjectMapper(@Nullable injector: Injector): JacksonScalaObjectMapperType =
    withBuilder.jacksonScalaObjectMapper(injector)

  /**
   * Return the given [[JacksonScalaObjectMapperType]] configured with the properties
   * set from this [[ScalaObjectMapperModule]] and the given (nullable) [[Injector]].
   *
   * @note caution: this mutates the given [[JacksonScalaObjectMapperType]].
   *
   * @return the updated [[JacksonScalaObjectMapperType]].
   */
  final def jacksonScalaObjectMapper(
    @Nullable injector: Injector,
    underlying: JacksonScalaObjectMapperType
  ): JacksonScalaObjectMapperType =
    withBuilder.jacksonScalaObjectMapper(injector, underlying)

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
   * @see [[com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS]]
   */
  protected def numbersAsStrings: Boolean = ScalaObjectMapper.DefaultNumbersAsStrings

  /**
   * @see [[ScalaObjectMapper.DefaultJacksonModules]]
   * @see [[ScalaObjectMapper.Builder.withDefaultJacksonModules]]
   */
  protected def defaultJacksonModules: Seq[JacksonModule] =
    ScalaObjectMapper.DefaultJacksonModules

  /**
   * @see [[ScalaObjectMapper.DefaultValidationProvider]]
   * @see [[ScalaObjectMapper.Builder.withValidationProvider]]
   */
  protected def validationProvider: ValidationProvider =
    ScalaObjectMapper.DefaultValidationProvider

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

  /** Convenience method for copying a [[JacksonScalaObjectMapperType]]  */
  protected final def copy(objectMapper: JacksonScalaObjectMapperType): JacksonScalaObjectMapperType =
    ObjectMapperCopier.copy(objectMapper)

  /* Private */

  // allows for overriding the injectable types via the provides method for injection
  protected final def provideScalaObjectMapper(
    injector: Injector,
    injectableTypes: Option[InjectableTypes]
  ): JacksonScalaObjectMapperType =
    withBuilder
      .withInjectableTypes(injectableTypes.getOrElse(NullInjectableTypes))
      .jacksonScalaObjectMapper(injector)

  /* Private */

  private[this] final def withBuilder: ScalaObjectMapper.Builder =
    builder
      .withPropertyNamingStrategy(this.propertyNamingStrategy)
      .withNumbersAsStrings(this.numbersAsStrings)
      .withSerializationInclude(this.serializationInclusion)
      .withSerializationConfig(this.serializationConfig)
      .withDeserializationConfig(this.deserializationConfig)
      .withDefaultJacksonModules(this.defaultJacksonModules)
      .withValidationProvider(this.validationProvider)
      .withAdditionalJacksonModules(this.additionalJacksonModules)
      .withAdditionalMapperConfigurationFn(this.additionalMapperConfiguration)

  /* NOTE: these methods are private as they are only meant to be called by the
     injector for providing these types. */

  // We explicitly add the `injectableTypes: Option[InjectableTypes]` as a parameter
  // to force the injector to resolve this type instead of attempting to read it from
  // the passed injector which may not be completely initialized here.
  @Singleton
  @Provides
  @SnakeCaseMapper
  private final def provideSnakeCaseObjectMapper(
    injector: Injector,
    injectableTypes: Option[InjectableTypes]
  ): ScalaObjectMapper =
    ScalaObjectMapper.snakeCaseObjectMapper(provideScalaObjectMapper(injector, injectableTypes))

  @Singleton
  @Provides
  @CamelCaseMapper
  private final def provideCamelCaseObjectMapper(
    injector: Injector,
    injectableTypes: Option[InjectableTypes]
  ): ScalaObjectMapper =
    ScalaObjectMapper.camelCaseObjectMapper(provideScalaObjectMapper(injector, injectableTypes))

  @Singleton
  @Provides
  private final def provideObjectMapper(
    injector: Injector,
    injectableTypes: Option[InjectableTypes]
  ): ScalaObjectMapper =
    new ScalaObjectMapper(provideScalaObjectMapper(injector, injectableTypes))
}
