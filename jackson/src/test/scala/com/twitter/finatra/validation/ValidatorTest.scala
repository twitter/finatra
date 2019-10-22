package com.twitter.finatra.validation

import com.twitter.finatra.json.internal.caseclass.validation.ValidationManager
import com.twitter.inject.Test
import com.twitter.inject.utils.AnnotationUtils
import java.lang.annotation.Annotation
import org.json4s.reflect.{ClassDescriptor, Reflector, classDescribable}

class ValidatorTest extends Test {

  val messageResolver = new ValidationMessageResolver
  val validationManager = new ValidationManager(messageResolver)

  def validate[A <: Annotation, V](
    clazz: Class[_],
    paramName: String,
    annotationClass: Class[A],
    value: V
  ): ValidationResult = {
    val annotation = getValidationAnnotation[A](clazz, paramName, annotationClass)
    validationManager.getValidator[A, V](annotation).isValid(value)
  }

  def getValidationAnnotations(clazz: Class[_], paramName: String): Seq[Annotation] = {
    val fieldNames: Seq[String] =
      Reflector
        .describe(classDescribable(clazz))
        .asInstanceOf[ClassDescriptor]
        .constructors.head
        .params.map(_.name)
    val annotations = AnnotationUtils.findAnnotations(clazz, fieldNames)

    for {
      field <- fieldNames
      paramAnnotations = annotations(field)
      if field.equals(paramName)
      annotation <- paramAnnotations
      if validationManager.isValidationAnnotation(annotation)
    } yield annotation
  }

  def getValidationAnnotation[A <: Annotation](
    clazz: Class[_],
    paramName: String,
    annotationClass: Class[A]
  ): A = {
    val annotations = getValidationAnnotations(clazz, paramName)
    findAnnotation(annotationClass, annotations)
  }

  def findAnnotation[A <: Annotation](
    annotationClass: Class[A],
    annotations: Seq[Annotation]
  ): A = {
    AnnotationUtils.findAnnotation(annotationClass, annotations) match {
      case Some(annotation) => annotation.asInstanceOf[A]
      case _ => throw new IllegalArgumentException("Unknown annotation: " + annotationClass)
    }
  }
}
