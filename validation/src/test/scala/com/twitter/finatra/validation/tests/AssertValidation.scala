package com.twitter.finatra.validation.tests

import com.twitter.inject.Test
import com.twitter.util.validation.ScalaValidator
import com.twitter.util.validation.engine.ConstraintViolationHelper
import jakarta.validation.{ConstraintViolation, ConstraintViolationException}
import scala.jdk.CollectionConverters._

private[tests] trait AssertValidation { self: Test =>
  protected def validator: ScalaValidator

  protected def assertValidation(
    obj: Any,
    withErrors: Seq[String] = Seq.empty[String]
  ): Unit = {
    if (withErrors.nonEmpty) {
      val e = intercept[ConstraintViolationException] {
        validator.verify(obj)
      }
      assertValidationException(e, withErrors)
    } else {
      validator.verify(obj)
    }
  }

  protected def assertValidationException(
    e: ConstraintViolationException,
    withErrors: Seq[String]
  ): Unit = {
    val violations: Iterable[ConstraintViolation[Any]] =
      e.getConstraintViolations.asScala.map(_.asInstanceOf[ConstraintViolation[Any]])
    violations.size should equal(withErrors.size)
    val sortedViolations: Seq[ConstraintViolation[Any]] =
      ConstraintViolationHelper.sortViolations(violations.toSet)
    for ((error, index) <- sortedViolations.zipWithIndex) {
      val withErrorMessage = withErrors(index)
      ConstraintViolationHelper.messageWithPath(error) should equal(withErrorMessage)
    }
  }
}
