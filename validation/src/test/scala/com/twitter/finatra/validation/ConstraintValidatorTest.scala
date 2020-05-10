package com.twitter.finatra.validation

import com.twitter.finatra.validation.internal.AnnotatedClass
import com.twitter.inject.Test
import java.lang.annotation.Annotation

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

  def getValidationAnnotations(clazz: Class[_], paramName: String): Array[Annotation] = {
    val AnnotatedClass(_, fields, _) = validator.getAnnotatedClass(clazz)
    fields
      .get(paramName).map(_.fieldValidators.map(_.annotation)).getOrElse(Array.empty[Annotation])
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
