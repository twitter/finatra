package com.twitter.finatra.validation.internal

import com.twitter.finatra.validation.{MethodValidation, Path}
import java.lang.reflect.Method

/**
 * A Finatra internal class that carries a [[Method]] along with its
 * [[MethodValidation]] annotation.
 *
 * Used by classes exposed to `private[finatra]`.
 */
private[finatra] case class AnnotatedMethod(
  name: Option[String],
  path: Path,
  method: Method,
  annotation: MethodValidation)
    extends AnnotatedMember
