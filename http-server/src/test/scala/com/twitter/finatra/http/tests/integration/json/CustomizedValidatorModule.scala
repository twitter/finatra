package com.twitter.finatra.http.tests.integration.json

import com.twitter.finatra.validation.ValidatorModule
import com.twitter.inject.Injector
import com.twitter.util.validation.ScalaValidator
import jakarta.validation.MessageInterpolator
import java.util.Locale

object CustomizedValidatorModule extends ValidatorModule {
  private val Message = "Whatever you provided is wrong."

  class CustomizedMessageInterpolator extends MessageInterpolator {
    override def interpolate(
      s: String,
      context: MessageInterpolator.Context
    ): String = Message

    override def interpolate(
      s: String,
      context: MessageInterpolator.Context,
      locale: Locale
    ): String = Message
  }

  override def configureValidator(
    injector: Injector,
    builder: ScalaValidator.Builder
  ): ScalaValidator.Builder =
    builder
      .withDescriptorCacheSize(512)
      .withMessageInterpolator(new CustomizedMessageInterpolator)
}
