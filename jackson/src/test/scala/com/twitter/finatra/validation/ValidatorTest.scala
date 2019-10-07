package com.twitter.finatra.validation

import com.twitter.finatra.json.internal.caseclass.jackson.CaseClassField._
import com.twitter.finatra.json.internal.caseclass.jackson.ConstructorParam
import com.twitter.finatra.json.internal.caseclass.utils.AnnotationUtils
import com.twitter.finatra.json.internal.caseclass.validation.ValidationManager
import com.twitter.inject.Test
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
    val constructorParams: Seq[ConstructorParam] =
      Reflector
        .describe(classDescribable(clazz))
        .asInstanceOf[ClassDescriptor]
        .constructors.head
        .params.map(param => ConstructorParam(param.name, param.argType))
    val annotations = findAnnotations(clazz, constructorParams)

    for {
      param <- constructorParams
      paramAnnotations = annotations(param.name)
      if param.name.equals(paramName)
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
      case Some(annotation) =>
        annotation.asInstanceOf[A]
      case _ =>
        throw new IllegalArgumentException("Unknown annotation: " + annotationClass)
    }
  }
}
