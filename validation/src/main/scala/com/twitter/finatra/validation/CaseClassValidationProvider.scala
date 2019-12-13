package com.twitter.finatra.validation
import java.lang.annotation.Annotation

private[finatra] object CaseClassValidationProvider extends CaseClassValidationProvider

private[finatra] class CaseClassValidationProvider extends ValidationProvider {

  override def apply(): CaseClassValidator = {
    val messageResolver = new ValidationMessageResolver
    new ValidationManager(messageResolver)
  }

  override val validationAnnotation: Class[_ <: Annotation] = classOf[Validation]
}
