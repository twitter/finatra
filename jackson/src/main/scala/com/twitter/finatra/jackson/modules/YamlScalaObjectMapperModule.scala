package com.twitter.finatra.jackson.modules

import com.google.inject.Injector
import com.twitter.util.jackson.{JacksonScalaObjectMapperType, ScalaObjectMapper}
import com.twitter.util.validation.ScalaValidator
import javax.annotation.Nullable

object YamlScalaObjectMapperModule extends YamlScalaObjectMapperModule {
  // java-friendly access to singleton
  def get(): this.type = this
}

/**
 * [[com.twitter.inject.TwitterModule]] to configure Jackson YAML object mappers. Extend this module to override defaults
 * or provide additional configuration to the bound [[ScalaObjectMapper]] instances.
 *
 * @see [[com.fasterxml.jackson.dataformat.yaml.YAMLFactory]]
 */
class YamlScalaObjectMapperModule extends ScalaObjectMapperModule {

  /** Return a [[JacksonScalaObjectMapperType]] configured from this [[ScalaObjectMapperModule]]. */
  override final def jacksonScalaObjectMapper: JacksonScalaObjectMapperType =
    withBuilder.yamlObjectMapper.underlying

  /** Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]]. */
  override final def objectMapper: ScalaObjectMapper = withBuilder.yamlObjectMapper

  /**
   * Return a [[JacksonScalaObjectMapperType]] configured from this [[ScalaObjectMapperModule]]
   * using the given (nullable) [[Injector]].
   *
   * @param injector a configured (nullable) [[Injector]].
   */
  override def jacksonScalaObjectMapper(
    @Nullable injector: Injector
  ): JacksonScalaObjectMapperType = {
    val withGuiceInjectableValues = configureGuiceInjectableValues(injector)
    withBuilder
      .withAdditionalMapperConfigurationFn(withGuiceInjectableValues)
      .yamlObjectMapper
      .underlying
  }

  /**
   * Return a [[ScalaObjectMapper]] configured from this [[ScalaObjectMapperModule]]
   * using the given (nullable) [[Injector]].
   *
   * @param injector a configured (nullable) [[Injector]].
   */
  override final def objectMapper(@Nullable injector: Injector): ScalaObjectMapper = {
    val withGuiceInjectableValues = configureGuiceInjectableValues(injector)
    withBuilder
      .withAdditionalMapperConfigurationFn(withGuiceInjectableValues)
      .yamlObjectMapper
  }

  /** Private API -- DO NOT OVERRIDE */

  override final protected[modules] def provideScalaObjectMapper(
    injector: Injector,
    validator: Option[ScalaValidator]
  ): ScalaObjectMapper = {
    provideConfiguredObjectMapperBuilder(injector, validator).yamlObjectMapper
  }
}
