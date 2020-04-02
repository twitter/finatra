package com.twitter.finatra.validation.internal

import java.lang.reflect.Field

/**
 * A Finatra internal class that carries a case class field with its annotations and the validators
 * needed to validate the annotations.
 *
 * @param field A case class [[Field]].
 * @param fieldValidators Stores validators needed to validate the Constraint
 *                        annotations defined for the field.
 */
private[finatra] case class AnnotatedField(field: Field, fieldValidators: Array[FieldValidator])
