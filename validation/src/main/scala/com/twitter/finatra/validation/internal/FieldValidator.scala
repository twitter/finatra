package com.twitter.finatra.validation.internal

import com.twitter.finatra.validation.ConstraintValidator
import java.lang.annotation.Annotation

/**
 * Validator and annotation pair of a case class field
 * @param constraintValidator A [[ConstraintValidator]] for a certain annotation
 * @param annotation The case class [[com.twitter.finatra.validation.Constraint]] annotation
 */
private[finatra] case class FieldValidator(
  constraintValidator: ConstraintValidator[_ <: Annotation, _],
  annotation: Annotation)
