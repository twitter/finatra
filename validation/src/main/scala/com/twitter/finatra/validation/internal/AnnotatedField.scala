package com.twitter.finatra.validation.internal

import com.twitter.finatra.validation.Path
import java.lang.reflect.Field

/**
 * A Finatra internal class that carries a case class field with its annotations and the validators
 * needed to validate the annotations.
 *
 * Used by classes exposed to `private[finatra]`.
 */
private[finatra] case class AnnotatedField(
  name: Option[String],
  path: Path,
  field: Option[Field],
  fieldValidators: Array[FieldValidator])
    extends AnnotatedMember
