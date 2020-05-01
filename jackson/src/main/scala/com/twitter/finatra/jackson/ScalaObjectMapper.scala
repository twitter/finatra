package com.twitter.finatra.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonGenerator.Feature
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import com.fasterxml.jackson.databind.{ObjectMapper => JacksonObjectMapper, _}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.{
  ScalaObjectMapper => JacksonScalaObjectMapper
}
import com.google.inject.Injector
import com.twitter.finatra.jackson.caseclass.{
  CaseClassJacksonModule,
  DefaultInjectableValues,
  InjectableTypes,
  NullInjectableTypes
}
import com.twitter.finatra.jackson.serde.{LongKeyDeserializers, SerDeSimpleModule}
import com.twitter.finatra.validation.Validator
import com.twitter.io.Buf
import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import javax.annotation.Nullable

object ScalaObjectMapper {

  /** The default [[Validator]] for a [[ScalaObjectMapper]] */
  private[jackson] val DefaultValidator: Validator = Validator()

  /** The default [[InjectableTypes]] for a [[ScalaObjectMapper]] */
  private[jackson] val DefaultInjectableTypes: InjectableTypes = NullInjectableTypes

  /** The default [[Feature.WRITE_NUMBERS_AS_STRINGS]] setting */
  private[jackson] val DefaultNumbersAsStrings: Boolean = false

  /** Framework modules need to be added 'last' so they can override existing ser/des */
  private[finatra] val DefaultJacksonModules: Seq[Module] =
    Seq(DefaultScalaModule, new JodaModule, LongKeyDeserializers, SerDeSimpleModule)

  /** The default [[PropertyNamingStrategy]] for a [[ScalaObjectMapper]] */
  private[jackson] val DefaultPropertyNamingStrategy: PropertyNamingStrategy =
    PropertyNamingStrategy.SNAKE_CASE

  /** The default [[JsonInclude.Include]] for serialization for a [[ScalaObjectMapper]] */
  private[jackson] val DefaultSerializationInclude: JsonInclude.Include =
    JsonInclude.Include.NON_ABSENT

  /** The default configuration for serialization as a `Map[SerializationFeature, Boolean]` */
  private[jackson] val DefaultSerializationConfig: Map[SerializationFeature, Boolean] =
    Map(
      SerializationFeature.WRITE_DATES_AS_TIMESTAMPS -> false,
      SerializationFeature.WRITE_ENUMS_USING_TO_STRING -> true)

  /** The default configuration for deserialization as a `Map[DeserializationFeature, Boolean]` */
  private[jackson] val DefaultDeserializationConfig: Map[DeserializationFeature, Boolean] =
    Map(
      DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES -> true,
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES -> false,
      DeserializationFeature.READ_ENUMS_USING_TO_STRING -> true,
      DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY -> true,
      DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY -> true /* see jackson-module-scala/issues/148 */
    )

  /** The default for configuring additional modules on the underlying [[JacksonScalaObjectMapperType]] */
  private[jackson] val DefaultAdditionalJacksonModules: Seq[Module] = Seq.empty[Module]

  /** The default for mutating the underlying [[JacksonScalaObjectMapperType]] with additional configuration */
  private[jackson] val DefaultAdditionalMapperConfigurationFn: JacksonObjectMapper => Unit =
    _ => Unit

  /** The default setting to enable case class validation during case class deserialization */
  private[jackson] val DefaultValidation: Boolean = true

  /**
   * When not using injection, this factory method can be used. Use of this method will
   * NOT configure the underlying mapper for supporting [[com.fasterxml.jackson.databind.InjectableValues]]
   * provided by the object graph. To configure the ability to "inject" values during deserialization
   * which come from the object graph a non-null [[Injector]] is required, please
   * see: [[apply(injector: Injector)]].
   *
   * @note the preferred way of obtaining a [[ScalaObjectMapper]] is through injection using
   *       the `ScalaObjectMapperModule`.
   * @return a new [[ScalaObjectMapper]] instance.
   *
   * @see [[ScalaObjectMapper(injector: Injector)]]
   * @see [[com.fasterxml.jackson.databind.InjectableValues]]
   */
  def apply(): ScalaObjectMapper =
    ScalaObjectMapper.builder.objectMapper

  /**
   * When not using injection, this factory method can be used but be aware that only
   * default [[com.fasterxml.jackson.databind.InjectableValues]] are supported via this
   * instantiation when given a non-null [[com.google.inject.Injector]]. By default only
   * [[com.fasterxml.jackson.databind.InjectableValues]] provided by the object graph
   * is supported during deserialization (case class fields annotated with `@Inject`).
   *
   * @note the preferred way of obtaining a [[ScalaObjectMapper]] is through injection
   *       using the `ScalaObjectMapperModule`.
   * @param injector a configured (nullable) [[Injector]].
   *
   * @return a new [[ScalaObjectMapper]] instance.
   *
   * @see [[com.fasterxml.jackson.databind.InjectableValues]]
   */
  def apply(@Nullable injector: Injector): ScalaObjectMapper =
    ScalaObjectMapper.builder.objectMapper(injector)

  /**
   * Creates a new [[ScalaObjectMapper]] wrapping an underlying [[JacksonScalaObjectMapperType]].
   *
   * @note this mutates the underlying mapper to configure it with the [[ScalaObjectMapper]] defaults.
   *
   * @param underlying the [[JacksonScalaObjectMapperType]] to wrap.
   *
   * @return a new [[ScalaObjectMapper]]
   */
  def apply(underlying: JacksonScalaObjectMapperType): ScalaObjectMapper =
    ScalaObjectMapper.builder.objectMapper(null, underlying)

  /**
   * Creates a new [[ScalaObjectMapper]] with the given [[Injector]]
   * wrapping an underlying [[JacksonScalaObjectMapperType]].
   *
   * @note the given [[Injector]] can be null.
   * @note this mutates the underlying mapper to configure it with the [[ScalaObjectMapper]] defaults.
   *
   * @param injector a configured (nullable) [[Injector]].
   * @param underlying the [[JacksonScalaObjectMapperType]] to wrap.
   *
   * @return a new [[ScalaObjectMapper]]
   */
  def apply(
    @Nullable injector: Injector,
    underlying: JacksonScalaObjectMapperType
  ): ScalaObjectMapper =
    ScalaObjectMapper.builder.objectMapper(injector, underlying)

  /**
   * Utility to create a new [[ScalaObjectMapper]] which simply wraps the given
   * [[JacksonScalaObjectMapperType]].
   *
   * @note the `underlying` mapper is not mutated to produce the new [[ScalaObjectMapper]] unlike
   *       the [[apply(underlying: JacksonScalaObjectMapperType)]] which mutates the underlying
   *       mapper to configure it as per the [[ScalaObjectMapper]] default.
   */
  final def objectMapper(underlying: JacksonScalaObjectMapperType): ScalaObjectMapper = {
    val objectMapperCopy = ObjectMapperCopier.copy(underlying)
    new ScalaObjectMapper(objectMapperCopy)
  }

  /**
   * Utility to create a new [[ScalaObjectMapper]] explicitly configured with
   * [[PropertyNamingStrategy.LOWER_CAMEL_CASE]] as a `PropertyNamingStrategy` wrapping the
   * given [[JacksonScalaObjectMapperType]].
   *
   * @note the `underlying` mapper is copied (not mutated) to produce the new [[ScalaObjectMapper]]
   *       with a [[PropertyNamingStrategy.LOWER_CAMEL_CASE]] PropertyNamingStrategy.
   */
  final def camelCaseObjectMapper(underlying: JacksonScalaObjectMapperType): ScalaObjectMapper = {
    val objectMapperCopy = ObjectMapperCopier.copy(underlying)
    objectMapperCopy.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
    new ScalaObjectMapper(objectMapperCopy)
  }

  /**
   * Utility to create a new [[ScalaObjectMapper]] explicitly configured with
   * [[PropertyNamingStrategy.SNAKE_CASE]] as a `PropertyNamingStrategy` wrapping the
   * given [[JacksonScalaObjectMapperType]].
   *
   * @note the `underlying` mapper is copied (not mutated) to produce the new [[ScalaObjectMapper]]
   *       with a [[PropertyNamingStrategy.SNAKE_CASE]] PropertyNamingStrategy.
   */
  final def snakeCaseObjectMapper(underlying: JacksonScalaObjectMapperType): ScalaObjectMapper = {
    val objectMapperCopy = ObjectMapperCopier.copy(underlying)
    objectMapperCopy.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    new ScalaObjectMapper(objectMapperCopy)
  }

  /**
   *
   * Build a new instance of a [[ScalaObjectMapper]].
   *
   * For example,
   * {{{
   *   ScalaObjectMapper.builder
   *    .withPropertyNamingStrategy(new PropertyNamingStrategy.UpperCamelCaseStrategy)
   *    .withNumbersAsStrings(true)
   *    .withAdditionalJacksonModules(...)
   *    .objectMapper
   * }}}
   *
   * or
   *
   * {{{
   *   val builder =
   *    ScalaObjectMapper.builder
   *      .withPropertyNamingStrategy(new PropertyNamingStrategy.UpperCamelCaseStrategy)
   *      .withNumbersAsStrings(true)
   *      .withAdditionalJacksonModules(...)
   *
   *   val mapper = builder.objectMapper
   *   val camelCaseMapper = builder.camelCaseObjectMapper
   * }}}
   *
   */
  def builder: ScalaObjectMapper.Builder = Builder()

  /**
   * A Builder for creating a new [[ScalaObjectMapper]]. E.g., to build a new instance of
   * a [[ScalaObjectMapper]].
   *
   * For example,
   * {{{
   *   ScalaObjectMapper.builder
   *     .withPropertyNamingStrategy(new PropertyNamingStrategy.UpperCamelCaseStrategy)
   *     .withNumbersAsStrings(true)
   *     .withAdditionalJacksonModules(...)
   *     .objectMapper
   * }}}
   *
   * or
   *
   * {{{
   *   val builder =
   *     ScalaObjectMapper.builder
   *       .withPropertyNamingStrategy(new PropertyNamingStrategy.UpperCamelCaseStrategy)
   *       .withNumbersAsStrings(true)
   *       .withAdditionalJacksonModules(...)
   *
   *     val mapper = builder.objectMapper
   *     val camelCaseMapper = builder.camelCaseObjectMapper
   * }}}
   */
  case class Builder private[jackson] (
    propertyNamingStrategy: PropertyNamingStrategy = DefaultPropertyNamingStrategy,
    numbersAsStrings: Boolean = DefaultNumbersAsStrings,
    serializationInclude: Include = DefaultSerializationInclude,
    serializationConfig: Map[SerializationFeature, Boolean] = DefaultSerializationConfig,
    deserializationConfig: Map[DeserializationFeature, Boolean] = DefaultDeserializationConfig,
    defaultJacksonModules: Seq[Module] = DefaultJacksonModules,
    validator: Option[Validator] = Some(DefaultValidator),
    injectableTypes: InjectableTypes = DefaultInjectableTypes,
    additionalJacksonModules: Seq[Module] = DefaultAdditionalJacksonModules,
    additionalMapperConfigurationFn: JacksonObjectMapper => Unit =
      DefaultAdditionalMapperConfigurationFn,
    validation: Boolean = DefaultValidation) {

    /* Public */

    /** Create a new [[ScalaObjectMapper]] from this [[Builder]]. */
    final def objectMapper: ScalaObjectMapper =
      new ScalaObjectMapper(jacksonScalaObjectMapper)

    /** Create a new [[JacksonScalaObjectMapperType]] from this [[Builder]]. */
    final def jacksonScalaObjectMapper: JacksonScalaObjectMapperType =
      jacksonScalaObjectMapper(null)

    /**
     * Create a new [[ScalaObjectMapper]] from this [[Builder]] with the given (nullable) [[Injector]].
     * @param injector a configured (nullable) [[Injector]].
     * @return a new [[ScalaObjectMapper]] instance configured from this [[Builder]].
     */
    final def objectMapper(@Nullable injector: Injector): ScalaObjectMapper =
      new ScalaObjectMapper(jacksonScalaObjectMapper(injector))

    /**
     * Create a new [[JacksonScalaObjectMapperType]] from this [[Builder]] with the
     * given (nullable) [[Injector]].
     * @param injector a configured (nullable) [[Injector]].
     * @return a new [[JacksonScalaObjectMapperType]] instance configured from this [[Builder]].
     */
    final def jacksonScalaObjectMapper(@Nullable injector: Injector): JacksonScalaObjectMapperType =
      jacksonScalaObjectMapper(
        injector,
        new JacksonObjectMapper with JacksonScalaObjectMapper
      )

    /**
     * Create a new [[ScalaObjectMapper]] from this [[Builder]] with the given (nullable) [[Injector]]
     * @param injector a configured (nullable) [[Injector]].
     * @param underlying the [[JacksonScalaObjectMapperType]] to wrap.
     * @return a new [[ScalaObjectMapper]] instance.
     */
    final def objectMapper(
      @Nullable injector: Injector,
      underlying: JacksonScalaObjectMapperType
    ): ScalaObjectMapper =
      new ScalaObjectMapper(jacksonScalaObjectMapper(injector, underlying))

    /**
     * Return a mutated instance of the given [[JacksonScalaObjectMapperType]] configured with the
     * given (nullable) [[Injector]].
     *
     * @param injector a configured (nullable) [[Injector]].
     * @param underlying the [[JacksonScalaObjectMapperType]] instance to use a basis for configuration.
     *
     * @return the updated [[JacksonScalaObjectMapperType]].
     */
    final def jacksonScalaObjectMapper(
      @Nullable injector: Injector,
      underlying: JacksonScalaObjectMapperType
    ): JacksonScalaObjectMapperType = {
      jacksonScalaObjectMapper(
        injector,
        underlying,
        modules = jacksonModules
      )
    }

    /* exposed for testing */
    private[finatra] final def jacksonScalaObjectMapper(
      @Nullable injector: Injector,
      underlying: JacksonScalaObjectMapperType,
      modules: Seq[Module]
    ): JacksonScalaObjectMapperType = {
      this.defaultMapperConfiguration(underlying)
      this.additionalMapperConfigurationFn(underlying)

      underlying.setPropertyNamingStrategy(this.propertyNamingStrategy)
      modules.foreach(underlying.registerModule)
      if (this.numbersAsStrings) {
        underlying.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true)
      }
      underlying.setInjectableValues(new DefaultInjectableValues(injector))

      underlying
    }

    /**
     * Creates a new [[ScalaObjectMapper]] explicitly configured with
     * [[PropertyNamingStrategy.LOWER_CAMEL_CASE]] as a `PropertyNamingStrategy`.
     */
    final def camelCaseObjectMapper: ScalaObjectMapper =
      ScalaObjectMapper.camelCaseObjectMapper(jacksonScalaObjectMapper)

    /**
     * Creates a new [[ScalaObjectMapper]] explicitly configured with
     * [[PropertyNamingStrategy.SNAKE_CASE]] as a `PropertyNamingStrategy`.
     */
    final def snakeCaseObjectMapper: ScalaObjectMapper =
      ScalaObjectMapper.snakeCaseObjectMapper(jacksonScalaObjectMapper)

    private[this] def defaultMapperConfiguration(mapper: JacksonObjectMapper): Unit = {
      /* Serialization Config */
      mapper.setDefaultPropertyInclusion(
        JsonInclude.Value.construct(serializationInclude, serializationInclude))
      mapper
        .configOverride(classOf[Option[_]])
        .setIncludeAsProperty(JsonInclude.Value.construct(serializationInclude, Include.ALWAYS))
      for ((feature, state) <- serializationConfig) {
        mapper.configure(feature, state)
      }

      /* Deserialization Config */
      for ((feature, state) <- deserializationConfig) {
        mapper.configure(feature, state)
      }
    }

    /** Order is important: default + case class module + any additional */
    private[this] def jacksonModules: Seq[Module] = {
      this.defaultJacksonModules ++
        Seq(
          new CaseClassJacksonModule(
            this.injectableTypes,
            if (this.validation) this.validator else None)) ++
        this.additionalJacksonModules
    }

    /* Builder Methods */

    /**
     * Configure a [[PropertyNamingStrategy]] for this [[Builder]].
     * @note the default is [[PropertyNamingStrategy.SNAKE_CASE]]
     * @see [[ScalaObjectMapper.DefaultPropertyNamingStrategy]]
     */
    final def withPropertyNamingStrategy(propertyNamingStrategy: PropertyNamingStrategy): Builder =
      Builder(
        propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )

    /**
     * Enable the [[Feature.WRITE_NUMBERS_AS_STRINGS]] for this [[Builder]].
     * @note the default is false.
     */
    final def withNumbersAsStrings(numbersAsStrings: Boolean): Builder =
      Builder(
        this.propertyNamingStrategy,
        numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )

    /**
     * Configure a [[JsonInclude.Include]] for serialization for this [[Builder]].
     * @note the default is [[JsonInclude.Include.NON_ABSENT]]
     * @see [[ScalaObjectMapper.DefaultSerializationInclude]]
     */
    final def withSerializationInclude(serializationInclude: Include): Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )

    /**
     * Set the serialization configuration for this [[Builder]] as a `Map` of `SerializationFeature`
     * to `Boolean` (enabled).
     * @note the default is described by [[ScalaObjectMapper.DefaultSerializationConfig]].
     * @see [[ScalaObjectMapper.DefaultSerializationConfig]]
     */
    final def withSerializationConfig(
      serializationConfig: Map[SerializationFeature, Boolean]
    ): Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )

    /**
     * Set the deserialization configuration for this [[Builder]] as a `Map` of `DeserializationFeature`
     * to `Boolean` (enabled).
     * @note this overwrites the default deserialization configuration of this [[Builder]].
     * @note the default is described by [[ScalaObjectMapper.DefaultDeserializationConfig]].
     * @see [[ScalaObjectMapper.DefaultDeserializationConfig]]
     */
    final def withDeserializationConfig(
      deserializationConfig: Map[DeserializationFeature, Boolean]
    ): Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )

    /**
     * Configure a [[Validator]] for this [[Builder]]
     * @see [[ScalaObjectMapper.DefaultValidator]]
     *
     * @note If you pass `withNoValidation` to the builder all case class validations will be
     *       bypassed, regardless of the `withValidator` configuration.
     */
    final def withValidator(validator: Validator): Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        Some(validator),
        this.injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )

    /**
     * Disable case class validation during case class deserialization
     *
     * @see [[ScalaObjectMapper.DefaultValidation]]
     * @note If you pass `withNoValidation` to the builder all case class validations will be
     *       bypassed, regardless of the `withValidator` configuration.
     */
    final def withNoValidation: Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        validation = false
      )

    /**
     * Configure an [[InjectableTypes]] for this [[Builder]].
     * @note the default is a [[NullInjectableTypes]]
     * @see [[ScalaObjectMapper.DefaultInjectableTypes]]
     */
    final def withInjectableTypes(injectableTypes: InjectableTypes): Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )

    /**
     * Configure the list of additional Jackson [[Module]]s for this [[Builder]].
     * @note this will overwrite (not append) the list additional Jackson [[Module]]s of this [[Builder]].
     */
    final def withAdditionalJacksonModules(additionalJacksonModules: Seq[Module]): Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )

    /**
     * Configure additional [[JacksonObjectMapper]] functionality for the underlying mapper of this [[Builder]].
     * @note this will overwrite any previously set function.
     */
    final def withAdditionalMapperConfigurationFn(mapperFn: JacksonObjectMapper => Unit): Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        this.defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        this.additionalJacksonModules,
        mapperFn,
        this.validation
      )

    /** Private method to allow changing of the default Jackson Modules for use from the `ScalaObjectMapperModule` */
    private[jackson] final def withDefaultJacksonModules(
      defaultJacksonModules: Seq[Module]
    ): Builder =
      Builder(
        this.propertyNamingStrategy,
        this.numbersAsStrings,
        this.serializationInclude,
        this.serializationConfig,
        this.deserializationConfig,
        defaultJacksonModules,
        this.validator,
        this.injectableTypes,
        this.additionalJacksonModules,
        this.additionalMapperConfigurationFn,
        this.validation
      )
  }
}

private[jackson] object ArrayElementsOnNewLinesPrettyPrinter extends DefaultPrettyPrinter {
  _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE
  override def createInstance(): DefaultPrettyPrinter = this
}

/**
 * A thin wrapper over a [[https://github.com/FasterXML/jackson-module-scala jackson-module-scala]]
 * [[com.fasterxml.jackson.module.scala.ScalaObjectMapper]]
 *
 * @note this API is inspired by the [[https://github.com/codahale/jerkson Jerkson]]
 *       [[https://github.com/codahale/jerkson/blob/master/src/main/scala/com/codahale/jerkson/Parser.scala Parser]]
 *
 * @param underlying a configured [[JacksonScalaObjectMapperType]]
 */
class ScalaObjectMapper (val underlying: JacksonScalaObjectMapperType) {
  assert(underlying != null, "Underlying ScalaObjectMapper cannot be null.")

  /**
   * Constructed [[ObjectWriter]] that will serialize objects using specified pretty printer
   * for indentation (or if null, no pretty printer).
   */
  lazy val prettyObjectMapper: ObjectWriter =
    underlying.writer(ArrayElementsOnNewLinesPrettyPrinter)

  /** Returns the currently configured [[PropertyNamingStrategy]] */
  def propertyNamingStrategy: PropertyNamingStrategy =
    underlying.getPropertyNamingStrategy

  /**
   * Factory method for constructing a [[com.fasterxml.jackson.databind.ObjectReader]] that will
   * read or update instances of specified type, [[T]].
   * @tparam T the type for which to create a [[com.fasterxml.jackson.databind.ObjectReader]]
   * @return the created [[com.fasterxml.jackson.databind.ObjectReader]].
   */
  def reader[T: Manifest]: ObjectReader =
    underlying.readerFor[T]

  /** Read a value from a [[Buf]] into a type [[T]]. */
  def parse[T: Manifest](buf: Buf): T =
    parse[T](Buf.ByteBuffer.Shared.extract(buf))

  /** Read a value from a [[ByteBuffer]] into a type [[T]]. */
  def parse[T: Manifest](byteBuffer: ByteBuffer): T = {
    val is = new ByteBufferBackedInputStream(byteBuffer)
    underlying.readValue[T](is)
  }

  /** Convert from a [[JsonNode]] into a type [[T]]. */
  def parse[T: Manifest](jsonNode: JsonNode): T =
    convert[T](jsonNode)

  /** Read a value from an [[InputStream]] (caller is responsible for closing the stream) into a type [[T]]. */
  def parse[T: Manifest](inputStream: InputStream): T =
    underlying.readValue[T](inputStream)

  /** Read a value from an Array[Byte] into a type [[T]]. */
  def parse[T: Manifest](bytes: Array[Byte]): T =
    underlying.readValue[T](bytes)

  /** Read a value from a String into a type [[T]]. */
  def parse[T: Manifest](string: String): T =
    underlying.readValue[T](string)

  /** Read a value from a [[JsonParser]] into a type [[T]]. */
  def parse[T: Manifest](jsonParser: JsonParser): T =
    underlying.readValue[T](jsonParser)

  /**
   * Convenience method for doing two-step conversion from given value, into an instance of given
   * value type, [[JavaType]] if (but only if!) conversion is needed. If given value is already of
   * requested type, the value is returned as is.
   *
   * This method is functionally similar to first serializing a given value into JSON, and then
   * binding JSON data into a value of the given type, but should be more efficient since full
   * serialization does not (need to) occur. However, the same converters (serializers,
   * deserializers) will be used for data binding, meaning the same object mapper configuration
   * works.
   *
   * Note: it is possible that in some cases behavior does differ from full
   * serialize-then-deserialize cycle. It is not guaranteed, however, that the behavior is 100%
   * the same -- the goal is just to allow efficient value conversions for structurally compatible
   * Objects, according to standard Jackson configuration.
   *
   * Further note that this functionality is not designed to support "advanced" use cases, such as
   * conversion of polymorphic values, or cases where Object Identity is used.
   *
   * @param from value from which to convert.
   * @param toValueType type to be converted into.
   * @return a new instance of type [[JavaType]] converted from the given [[Any]] type.
   */
  def convert(from: Any, toValueType: JavaType): AnyRef = {
    try {
      underlying.convertValue(from, toValueType)
    } catch {
      case e: IllegalArgumentException if e.getCause != null =>
        throw e.getCause
    }
  }

  /**
   * Convenience method for doing two-step conversion from a given value, into an instance of a given
   * type, [[T]]]. This is functionality equivalent to first serializing the given value into JSON,
   * then binding JSON data into a value of the given type, but may be executed without fully
   * serializing into JSON. The same converters (serializers, deserializers) will be used for
   * data binding, meaning the same object mapper configuration works.
   *
   * Note: when Finatra's [[com.twitter.finatra.jackson.caseclass.exceptions.CaseClassMappingException]]
   * is thrown inside of the the `ObjectMapper#convertValue` method, Jackson wraps the exception
   * inside of an [[IllegalArgumentException]]. As such we unwrap to restore the original
   * exception here. The wrapping occurs because the [[com.twitter.finatra.jackson.caseclass.exceptions.CaseClassMappingException]]
   * is a sub-type of [[java.io.IOException]] (through extension of [[com.fasterxml.jackson.databind.JsonMappingException]]
   * --> [[com.fasterxml.jackson.core.JsonProcessingException]] --> [[java.io.IOException]].
   *
   * @param any the value to be converted.
   * @tparam T the type to which to be converted.
   * @return a new instance of type [[T]] converted from the given [[Any]] type.
   *
   * @see [[https://github.com/FasterXML/jackson-databind/blob/d70b9e65c5e089094ec7583fa6a38b2f484a96da/src/main/java/com/fasterxml/jackson/databind/ObjectMapper.java#L2167]]
   */
  def convert[T: Manifest](any: Any): T = {
    try {
      underlying.convertValue[T](any)
    } catch {
      case e: IllegalArgumentException if e.getCause != null =>
        throw e.getCause
    }
  }

  /**
   * Method that can be used to serialize any value as JSON output, using the output stream
   * provided (using an encoding of [[com.fasterxml.jackson.core.JsonEncoding#UTF8]].
   *
   * Note: this method does not close the underlying stream explicitly here; however, the
   * [[com.fasterxml.jackson.core.JsonFactory]] this mapper uses may choose to close the stream
   * depending on its settings (by default, it will try to close it when
   * [[com.fasterxml.jackson.core.JsonGenerator]] constructed is closed).
   *
   * @param any the value to serialize.
   * @param outputStream the [[OutputStream]] to which to serialize.
   */
  def writeValue(any: Any, outputStream: OutputStream): Unit =
    underlying.writeValue(outputStream, any)

  /**
   * Method that can be used to serialize any value as a `Array[Byte]`. Functionally equivalent
   * to calling [[JacksonObjectMapper#writeValue(Writer,Object)]] with a [[java.io.ByteArrayOutputStream]] and
   * getting bytes, but more efficient. Encoding used will be UTF-8.
   *
   * @param any the value to serialize.
   * @return the `Array[Byte]` representing the serialized value.
   */
  def writeValueAsBytes(any: Any): Array[Byte] =
    underlying.writeValueAsBytes(any)

  /**
   * Method that can be used to serialize any value as a String. Functionally equivalent to calling
   * [[JacksonObjectMapper#writeValue(Writer,Object)]] with a [[java.io.StringWriter]]
   * and constructing String, but more efficient.
   *
   * @param any the value to serialize.
   * @return the String representing the serialized value.
   */
  def writeValueAsString(any: Any): String =
    underlying.writeValueAsString(any)

  /**
   * Method that can be used to serialize any value as a pretty printed String. Uses the
   * [[prettyObjectMapper]] and calls [[writeValueAsString(any: Any)]] on the given value.
   *
   * @param any the value to serialize.
   * @return the pretty printed String representing the serialized value.
   *
   * @see [[prettyObjectMapper]]
   * @see [[writeValueAsString(any: Any)]]
   */
  def writePrettyString(any: Any): String = any match {
    case str: String =>
      val jsonNode = underlying.readValue[JsonNode](str)
      prettyObjectMapper.writeValueAsString(jsonNode)
    case _ =>
      prettyObjectMapper.writeValueAsString(any)
  }

  /**
   * Method that can be used to serialize any value as a [[Buf]]. Functionally equivalent
   * to calling [[writeValueAsBytes(any: Any)]] and then wrapping the results in an
   * "owned" [[Buf.ByteArray]].
   *
   * @param any the value to serialize.
   * @return the [[Buf.ByteArray.Owned]] representing the serialized value.
   *
   * @see [[writeValueAsBytes(any: Any)]]
   * @see [[Buf.ByteArray.Owned]]
   */
  def writeValueAsBuf(any: Any): Buf =
    Buf.ByteArray.Owned(underlying.writeValueAsBytes(any))

  /**
   * Convenience method for doing the multi-step process of serializing a `Map[String, String]`
   * to a [[Buf]].
   *
   * @param stringMap the `Map[String, String]` to convert.
   * @return the [[Buf.ByteArray.Owned]] representing the serialized value.
   */
  // optimized
  def writeStringMapAsBuf(stringMap: Map[String, String]): Buf = {
    val os = new ByteArrayOutputStream()

    val jsonGenerator = underlying.getFactory.createGenerator(os)
    try {
      jsonGenerator.writeStartObject()
      for ((key, value) <- stringMap) {
        jsonGenerator.writeStringField(key, value)
      }
      jsonGenerator.writeEndObject()
      jsonGenerator.flush()

      Buf.ByteArray.Owned(os.toByteArray)
    } finally {
      jsonGenerator.close()
    }
  }

  /**
   * Method for registering a module that can extend functionality provided by this mapper; for
   * example, by adding providers for custom serializers and deserializers.
   *
   * @note this mutates the [[underlying]] ObjectMapper of this mapper.
   *
   * @param module [[Module]] to register.
   */
  def registerModule(module: Module): JacksonObjectMapper =
    underlying.registerModule(module)
}
