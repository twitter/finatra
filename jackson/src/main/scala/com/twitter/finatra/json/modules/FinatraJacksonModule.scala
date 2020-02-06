package com.twitter.finatra.json.modules

import com.google.inject.{Injector, Provides}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.caseclass.InjectableTypes
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.annotations.{CamelCaseMapper, SnakeCaseMapper}
import javax.inject.Singleton

@deprecated(
  "Use com.twitter.finatra.jackson.modules.ScalaObjectMapperModule directly.",
  "2019-11-19")
object FinatraJacksonModule extends FinatraJacksonModule

@deprecated(
  "Use com.twitter.finatra.jackson.modules.ScalaObjectMapperModule directly.",
  "2019-11-19")
class FinatraJacksonModule extends ScalaObjectMapperModule {

  @Singleton
  @Provides
  @SnakeCaseMapper
  private final def provideSnakeCaseFinatraObjectMapper(
    injector: Injector,
    injectableTypes: Option[InjectableTypes]
  ): FinatraObjectMapper =
    FinatraObjectMapper(
      ScalaObjectMapper
        .snakeCaseObjectMapper(provideScalaObjectMapper(injector, injectableTypes)).underlying)

  @Singleton
  @Provides
  @CamelCaseMapper
  private final def provideCamelCaseFinatraObjectMapper(
    injector: Injector,
    injectableTypes: Option[InjectableTypes]
  ): FinatraObjectMapper =
    FinatraObjectMapper(
      ScalaObjectMapper
        .camelCaseObjectMapper(provideScalaObjectMapper(injector, injectableTypes)).underlying)

  @Singleton
  @Provides
  private final def provideFinatraObjectMapper(
    injector: Injector,
    injectableTypes: Option[InjectableTypes]
  ): FinatraObjectMapper = FinatraObjectMapper(provideScalaObjectMapper(injector, injectableTypes))
}
