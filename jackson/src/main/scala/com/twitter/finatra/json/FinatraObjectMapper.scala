package com.twitter.finatra.json

import com.fasterxml.jackson.databind.{ObjectMapperCopier, PropertyNamingStrategy}
import com.google.inject.Injector
import com.twitter.finatra.jackson.{JacksonScalaObjectMapperType, ScalaObjectMapper}

@deprecated("Use com.twitter.finatra.jackson.ScalaObjectMapper", "2019-10-10")
object FinatraObjectMapper {

  /**
   * When not using injection, this factory method can be used but be aware that only
   * default [[com.fasterxml.jackson.databind.InjectableValues]] are supported via this
   * instantiation when given a non-null [[com.google.inject.Injector]]. By default only
   * [[com.fasterxml.jackson.databind.InjectableValues]] provided by the object graph
   * is supported during deserialization (case class fields annotated with `@Inject`)
   * when provided a non-null [[Injector]].
   *
   * @note the preferred way of obtaining a [[FinatraObjectMapper]] is through injection
   *       using the FinatraJacksonModule.
   * @return a new [[FinatraObjectMapper]] instance
   *
   * @see [[com.fasterxml.jackson.databind.InjectableValues]]
   */
  @deprecated("Use com.twitter.finatra.jackson.ScalaObjectMapper.apply(injector)", "2019-10-10")
  def create(injector: Injector = null): FinatraObjectMapper =
    new FinatraObjectMapper(ScalaObjectMapper(injector).underlying)

  @deprecated("Use ScalaObjectMapper.camelCaseObjectMapper(underlying)", "2019-11-19")
  final def camelCaseObjectMapper(underlying: JacksonScalaObjectMapperType): FinatraObjectMapper = {
    val objectMapperCopy = ObjectMapperCopier.copy(underlying)
    objectMapperCopy.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
    new FinatraObjectMapper(objectMapperCopy)
  }

  @deprecated("Use ScalaObjectMapper.snakeCaseObjectMapper(underlying)", "2019-11-19")
  final def snakeCaseObjectMapper(underlying: JacksonScalaObjectMapperType): FinatraObjectMapper = {
    val objectMapperCopy = ObjectMapperCopier.copy(underlying)
    objectMapperCopy.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    new FinatraObjectMapper(objectMapperCopy)
  }
}

@deprecated("Use com.twitter.finatra.jackson.ScalaObjectMapper", "2019-10-10")
case class FinatraObjectMapper(override val underlying: JacksonScalaObjectMapperType)
    extends ScalaObjectMapper(underlying)
