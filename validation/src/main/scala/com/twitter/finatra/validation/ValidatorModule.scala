package com.twitter.finatra.validation

import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.validation.ScalaValidator
import javax.inject.Singleton

object ValidatorModule extends ValidatorModule {
  // java-friendly access to singleton
  def get(): this.type = this
}

/**
 * A [[TwitterModule]] to provide a [[ScalaValidator]] with default [[jakarta.validation.ConstraintValidator]]s.
 *
 * Extend this module to override defaults of the bound [[ScalaValidator]] instance.
 *
 * Example:
 *
 * {{{
 *    import com.twitter.finatra.validation.{ScalaValidator, ValidatorModule}
 *    import com.twitter.inject.Injector
 *
 *    object CustomizedValidatorModule extends ValidatorModule {
 *      override def configureValidator(injector: Injector, builder: ScalaValidator.Builder): ScalaValidator.Builder =
 *        builder
 *          .withDescriptorCacheSize(512)
 *          .withConstraintMapping(???)
 * }
 * }}}
 */
class ValidatorModule extends TwitterModule {

  private[this] val baseValidator: ScalaValidator.Builder = ScalaValidator.builder

  /**
   * Override this method to build an instance of [[ScalaValidator]], the new ScalaValidator will override the
   * default one that is bound to the object graph.
   *
   * @return a configured [[ScalaValidator.Builder]] to create a [[ScalaValidator]] instance.
   */
  protected def configureValidator(
    injector: Injector,
    builder: ScalaValidator.Builder
  ): ScalaValidator.Builder = builder

  @Provides
  @Singleton
  private final def provideValidator(injector: Injector): ScalaValidator =
    configureValidator(injector, baseValidator).validator
}
