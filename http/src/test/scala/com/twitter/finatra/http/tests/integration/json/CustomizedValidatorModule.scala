package com.twitter.finatra.http.tests.integration.json

import com.twitter.finatra.validation.{MessageResolver, Validator, ValidatorModule}
import com.twitter.inject.Injector
import java.lang.annotation.Annotation

object CustomizedValidatorModule extends ValidatorModule {

  override def configureValidator(
    injector: Injector,
    builder: Validator.Builder
  ): Validator.Builder =
    builder
      .withCacheSize(512)
      .withMessageResolver(new CustomizedMessageResolver())
}

class CustomizedMessageResolver extends MessageResolver {
  override def resolve(clazz: Class[_ <: Annotation], values: Any*): String =
    "Whatever you provided is wrong."
}
