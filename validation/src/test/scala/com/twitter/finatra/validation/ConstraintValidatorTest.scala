package com.twitter.finatra.validation

import com.twitter.inject.Test
import java.lang.annotation.Annotation

/**
 * Utility for testing custom ConstraintValidators.
 */
abstract class ConstraintValidatorTest extends Test {
  protected def messageResolver: MessageResolver = new MessageResolver
  protected def validator: Validator = Validator(messageResolver)

  def validate[A <: Annotation, V](
    clazz: Class[_],
    paramName: String,
    annotationClass: Class[A],
    value: V
  ): ValidationResult = {
    val annotation = getValidationAnnotation[A](clazz, paramName, annotationClass)
    validator
      .findFieldValidator[V](annotation).constraintValidator.asInstanceOf[
        ConstraintValidator[A, V]].isValid(annotation, value)
  }

  private[this] def getValidationAnnotations(
    clazz: Class[_],
    fieldName: String
  ): Seq[Annotation] =
    validator.findAnnotatedClass(clazz).getAnnotationsForAnnotatedMember(fieldName).toIndexedSeq

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
