package com.twitter.finatra.json

import com.twitter.finatra.json.internal.caseclass.jackson.CaseClassField._
import com.twitter.finatra.json.internal.caseclass.reflection.CaseClassSigParser._
import com.twitter.finatra.json.internal.caseclass.utils.AnnotationUtils
import com.twitter.finatra.json.internal.caseclass.validation.ValidationManager
import com.twitter.finatra.validation.{ValidationMessageResolver, ValidationResult}
import java.lang.annotation.Annotation
import org.joda.time.DateTimeZone
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

@RunWith(classOf[JUnitRunner])
class ValidatorTest extends WordSpec with Matchers {
  DateTimeZone.setDefault(DateTimeZone.UTC)

  val messageResolver = new ValidationMessageResolver
  val validationManager = new ValidationManager(messageResolver)

  def validate[A <: Annotation, V](clazz: Class[_], paramName: String, annotationClass: Class[A], value: V): ValidationResult = {
    val annotation = getValidationAnnotation[A](clazz, paramName, annotationClass)
    validationManager.getValidator[A, V](annotation).isValid(value)
  }

  def getValidationAnnotations(clazz: Class[_], paramName: String): Seq[Annotation] = {
    for {
      (param, annotations) <- parseConstructorParams(clazz).zip(constructorAnnotations(clazz))
      if param.name.equals(paramName)
      annotation <- annotations
      if validationManager.isValidationAnnotation(annotation)
    } yield annotation
  }

  def getValidationAnnotation[A <: Annotation](clazz: Class[_], paramName: String, annotationClass: Class[A]): A = {
    val annotations = getValidationAnnotations(clazz, paramName)
    findAnnotation(annotationClass, annotations)
  }

  def findAnnotation[A <: Annotation](annotationClass: Class[A], annotations: Seq[Annotation]): A = {
    AnnotationUtils.findAnnotation(annotationClass, annotations) match {
      case Some(annotation) =>
        annotation.asInstanceOf[A]
      case _ =>
        throw new IllegalArgumentException("Unknown annotation: " + annotationClass)
    }
  }
}
