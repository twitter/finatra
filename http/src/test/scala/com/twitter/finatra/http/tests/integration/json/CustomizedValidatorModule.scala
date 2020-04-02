package com.twitter.finatra.http.tests.integration.json

import com.google.inject.Provides
import com.twitter.finatra.validation.{MessageResolver, Validator}
import com.twitter.inject.{Injector, TwitterModule}
import java.lang.annotation.Annotation
import javax.inject.Singleton

object CustomizedValidatorModule extends TwitterModule {

  @Provides
  @Singleton
  private final def providesValidator(injector: Injector): Validator =
    Validator.builder.withMessageResolver(new CustomizedMessageResolver()).build()
}

class CustomizedMessageResolver extends MessageResolver {
  override def resolve(clazz: Class[_ <: Annotation], values: Any*): String =
    "Whatever you provided is wrong."
}
