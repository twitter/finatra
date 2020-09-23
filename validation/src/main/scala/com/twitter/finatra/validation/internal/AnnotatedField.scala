package com.twitter.finatra.validation.internal

import java.lang.reflect.Field

/**
 * A Finatra internal class that carries a case class field with its annotations and the validators
 * needed to validate the annotations.
 *
 * @param field An optional case class [[Field]]. Can be [[None]] in the case of an annotated static
 *              or secondary constructor arg.
 * @param fieldValidators Stores validators needed to validate the Constraint
 *                        annotations defined for the field.
 */
private[finatra] case class AnnotatedField(
  field: Option[Field],
  fieldValidators: Array[FieldValidator])
