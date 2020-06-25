package com.twitter.finatra.validation

import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton

object ValidatorModule extends ValidatorModule {
  // java-friendly access to singleton
  def get(): TwitterModule = this
}

/**
 * A [[TwitterModule]] to provide a [[Validator]] with with default [[MessageResolver]] and
 * default [[com.twitter.finatra.validation.ConstraintValidator]]s.
 *
 * Extend this module to override defaults of the bound [[Validator]] instance.
 *
 * Example:
 *
 * {{{
 *    import com.twitter.finatra.validation.{Validator, ValidatorModule}
 *    import com.twitter.inject.Injector
 *
 *    object CustomizedValidatorModule extends ValidatorModule {
 *      override def configureValidator(injector: Injector, builder: Validator.Builder): Validator.Builder =
 *        builder
 *          .withCacheSize(512)
 *          .withMessageResolver(new CustomizedMessageResolver())
 * }
 * }}}
 */
class ValidatorModule extends TwitterModule {

  private[this] val baseValidator: Validator.Builder = Validator.builder

  /**
   * Override this method to build an instance of [[Validator]], the new Validator will override the
   * default one that is bond to the object graph.
   *
   * @return a configured [[Validator.Builder]] to create a [[Validator]] instance.
   */
  protected def configureValidator(
    injector: Injector,
    builder: Validator.Builder
  ): Validator.Builder = builder

  @Provides
  @Singleton
  private final def provideValidator(injector: Injector): Validator =
    configureValidator(injector, baseValidator).build()
}
