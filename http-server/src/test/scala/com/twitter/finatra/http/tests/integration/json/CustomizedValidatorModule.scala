package com.twitter.finatra.http.tests.integration.json

import com.twitter.finatra.validation.{MessageResolver, Validator, ValidatorModule}
import com.twitter.inject.Injector
import java.lang.annotation.Annotation
import scala.reflect.ClassTag

object CustomizedValidatorModule extends ValidatorModule {

  override def configureValidator(
    injector: Injector,
    builder: Validator.Builder
  ): Validator.Builder =
    builder
      .withCacheSize(512)
      .withMessageResolver(new CustomizedMessageResolver)
}

private object CustomizedMessageResolver {
  private val Message = "Whatever you provided is wrong."
}

class CustomizedMessageResolver extends MessageResolver {
  import CustomizedMessageResolver._

  override def resolve(clazz: Class[_ <: Annotation], values: Any*): String = Message
  override def resolve[Ann <: Annotation: ClassTag](values: Any*): String = Message
}
