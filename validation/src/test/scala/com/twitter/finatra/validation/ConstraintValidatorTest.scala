package com.twitter.finatra.validation

import com.twitter.inject.Test
import com.twitter.util.validation.ScalaValidator
import com.twitter.util.validation.metadata.PropertyDescriptor
import jakarta.validation.ConstraintViolation
import java.lang.annotation.Annotation

/**
 * Utility for testing custom ConstraintValidators.
 */
abstract class ConstraintValidatorTest extends Test {
  protected def validator: ScalaValidator = ScalaValidator.builder.validator

  protected def validate[A <: Annotation, T: Manifest](
    clazz: Class[_],
    paramName: String,
    value: Any
  ): Set[ConstraintViolation[T]] =
    validator.validateValue(clazz.asInstanceOf[Class[T]], paramName, value)

  private[this] def getValidationAnnotations(
    clazz: Class[_],
    fieldName: String
  ): Seq[Annotation] =
    validator.getConstraintsForClass(clazz).members.get(fieldName) match {
      case Some(memberDescriptor: PropertyDescriptor) =>
        memberDescriptor.annotations
      case _ => Seq.empty
    }

  def getValidationAnnotation[A <: Annotation](
    clazz: Class[_],
    paramName: String,
    annotationClass: Class[A]
  ): A = {
    val annotations = getValidationAnnotations(clazz, paramName)
    findAnnotation(annotationClass, annotations)
  }

  private def findAnnotation[A <: Annotation](
    annotationClass: Class[A],
    annotations: Seq[Annotation]
  ): A = {
    val annotationOpt = annotations.find { annotation =>
      annotation.annotationType() == annotationClass
    }
    annotationOpt match {
      case Some(annotation) =>
        annotation.asInstanceOf[A]
      case _ =>
        throw new IllegalArgumentException("Unknown annotation: " + annotationClass)
    }
  }
}
