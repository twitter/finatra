package com.twitter.finatra.validation.constraints

import com.twitter.finatra.validation.ErrorCode
import com.twitter.util.validation.constraintvalidation.TwitterConstraintValidatorContext
import jakarta.validation.{ConstraintValidator, ConstraintValidatorContext, UnexpectedTypeException}
import scala.jdk.CollectionConverters._

/**
 * The validator for [[NotEmpty]] annotation. Validate if a given value is not empty.
 *
 * @note this does not guarantee that items within a "not empty" container are themselves
 *       not null or empty. E.g., a `Seq[String]` may not be empty but some of the contained
 *       Strings may be the empty string or even null. Likewise, a "not empty" `Option`,
 *       could be a `Some(null)`, etc.
 */
@deprecated("Users should prefer to use standard constraints.", "2021-03-05")
private[validation] class NotEmptyConstraintValidator extends ConstraintValidator[NotEmpty, Any] {

  override def isValid(
    obj: Any,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = obj match {
    case arrayValue: Array[_] =>
      handleInvalid(arrayValue.nonEmpty, constraintValidatorContext)
    case map: Map[_, _] =>
      handleInvalid(map.nonEmpty, constraintValidatorContext)
    case traversableValue: Iterable[_] =>
      handleInvalid(traversableValue.nonEmpty, constraintValidatorContext)
    case iterableWrapper: java.util.Collection[_] =>
      handleInvalid(iterableWrapper.asScala.nonEmpty, constraintValidatorContext)
    case stringValue: String =>
      handleInvalid(stringValue.nonEmpty, constraintValidatorContext)
    case _ =>
      throw new UnexpectedTypeException(
        s"Class [${obj.getClass.getName}] is not supported by ${this.getClass.getName}")
  }

  /* Private */

  private[this] def handleInvalid(
    valid: => Boolean,
    constraintValidatorContext: ConstraintValidatorContext
  ): Boolean = {
    if (!valid) {
      TwitterConstraintValidatorContext
        .withDynamicPayload(ErrorCode.ValueCannotBeEmpty)
        .withMessageTemplate("cannot be empty")
        .addConstraintViolation(constraintValidatorContext)
    }
    valid
  }
}
