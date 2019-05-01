package com.twitter.finatra.json.internal.caseclass.validation

import com.twitter.finatra.validation.ValidationMessageResolver

private[json] object DefaultValidationProvider extends ValidationProvider {

  override def apply(): CaseClassValidator = {
    val messageResolver = new ValidationMessageResolver
    new ValidationManager(messageResolver)
  }

}
