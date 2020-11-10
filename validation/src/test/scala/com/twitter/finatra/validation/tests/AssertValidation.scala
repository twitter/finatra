package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.{ValidationException, Validator}
import com.twitter.inject.Test

private[tests] trait AssertValidation { self: Test =>

  protected def validator: Validator

  protected def assertValidation(
    obj: Any,
    withErrors: Seq[String] = Seq.empty[String]
  ): Unit = {
    if (withErrors.nonEmpty) {
      val e = intercept[ValidationException] {
        validator.verify(obj)
      }
      assertValidationException(e, withErrors)
    } else {
      validator.verify(obj)
    }
  }

  protected def assertValidationException(
    e: ValidationException,
    withErrors: Seq[String]
  ): Unit = {
    e.errors.size should equal(withErrors.size)
    for ((error, index) <- e.errors.zipWithIndex) {
      val withErrorMessage = withErrors(index)
      error.message should equal(withErrorMessage)
    }
  }
}
