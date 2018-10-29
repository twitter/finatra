package com.twitter.finatra.json.internal.caseclass.validation

import com.twitter.finatra.validation.ValidationResult._
import com.twitter.finatra.validation.{
  MethodValidation,
  Validation,
  ValidationMessageResolver,
  ValidationResult,
  Validator
}
import java.lang.annotation.Annotation
import java.lang.reflect.Method
import scala.collection.mutable

class ValidationManager(validationMessageResolver: ValidationMessageResolver)
    extends CaseClassValidator {

  private val validatorMap = mutable.Map[Annotation, Validator[_, _]]()

  private val methodValidationMap = mutable.Map[Class[_], Seq[Method]]()

  /* Public */

  /**
   * Validate a field's value according to the field's validation annotations
   * @return Failed ValidationResults
   */
  override def validateField[V](
    fieldValue: V,
    fieldValidationAnnotations: Seq[Annotation]
  ): Seq[ValidationResult] = {
    for {
      annotation <- fieldValidationAnnotations
      result = isValid(fieldValue, findValidator[V](annotation))
      if !result.isValid
    } yield result
  }

  /**
   * Validate an object using @MethodValidation annotated methods
   * @return Failed ValidationResults
   */
  override def validateObject(obj: Any): Seq[ValidationResult] = {
    for {
      method <- findMethodValidations(obj.getClass)
      result = method.invoke(obj).asInstanceOf[ValidationResult]
      if !result.isValid
    } yield result
  }

  def getValidator[A <: Annotation, V](annotation: A): Validator[A, V] = {
    val validatorClass = getValidatedBy[A, V](annotation.annotationType().asInstanceOf[Class[A]])
    createValidator(validatorClass, annotation)
  }

  def getMethodValidations(clazz: Class[_]): Seq[Method] = {
    for {
      method <- clazz.getMethods
      annotation <- method.getAnnotations
      if isMethodValidationAnnotation(annotation)
    } yield method
  }

  def isValidationAnnotation(annotation: Annotation): Boolean = {
    annotation.annotationType.isAnnotationPresent(classOf[Validation])
  }

  def isMethodValidationAnnotation(annotation: Annotation): Boolean = {
    annotation.annotationType == classOf[MethodValidation]
  }

  /* Private */

  private def isValid[V](value: V, validator: Validator[_, V]): ValidationResult = {
    value match {
      case _: Option[_] =>
        isValidOption(value.asInstanceOf[Option[V]], validator)
      case _ =>
        validator.isValid(value)
    }
  }

  private def isValidOption[V](value: Option[V], validator: Validator[_, V]): ValidationResult = {
    value match {
      case Some(actualVal) =>
        validator.isValid(actualVal)
      case _ =>
        Valid
    }
  }

  private def getValidatedBy[A <: Annotation, V](
    annotationClass: Class[A]
  ): Class[Validator[A, V]] = {
    val validationAnnotation = annotationClass.getAnnotation(classOf[Validation])
    if (validationAnnotation == null)
      throw new IllegalArgumentException("Missing annotation: " + classOf[Validation])
    else
      validationAnnotation.validatedBy().asInstanceOf[Class[Validator[A, V]]]
  }

  private def findValidator[V](annotation: Annotation): Validator[_, V] = {
    validatorMap
      .getOrElseUpdate(annotation, getValidator(annotation))
      .asInstanceOf[Validator[_, V]]
  }

  private def findMethodValidations(clazz: Class[_]): Seq[Method] = {
    methodValidationMap.getOrElseUpdate(clazz, getMethodValidations(clazz))
  }

  private def createValidator[A <: Annotation, V](
    validatorClass: Class[Validator[A, V]],
    annotation: A
  ): Validator[A, V] = {
    try {
      validatorClass
        .getConstructor(validationMessageResolver.getClass, annotation.annotationType())
        .newInstance(validationMessageResolver, annotation)
    } catch {
      case _: NoSuchMethodException =>
        throw new IllegalArgumentException(
          "Validator [%s] does not contain a two-arg constructor with parameter types: [%s, %s]"
            .format(validatorClass, validationMessageResolver.getClass, annotation.annotationType())
        )
    }
  }
}
