package com.twitter.finatra.validation

import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import java.lang.annotation.Annotation
import java.lang.reflect.Method
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

private[twitter] class ValidationManager(
  validationMessageResolver: ValidationMessageResolver
) extends CaseClassValidator {

  private[this] val validatorMap = mutable.Map[Annotation, Validator[_, _]]()
  private[this] val methodValidationMap = mutable.Map[Class[_], Array[(Method, Annotation)]]()

  /* Public */

  /**
   * Validate a field's value according to the field's validation annotations
   * @return Failed ValidationResults
   */
  // optimized
  private[finatra] override def validateField[V](
    fieldValue: V,
    fieldValidationAnnotations: Array[Annotation]
  ): Seq[ValidationResult] = {

    val results = new ArrayBuffer[ValidationResult]()
    var index = 0
    while (index < fieldValidationAnnotations.length) {
      val annotation = fieldValidationAnnotations(index)
      val result = isValid(fieldValue, findValidator[V](annotation))
      val resultWithAnnotation = result match {
        case invalid @ Invalid(_, _, _) =>
          invalid.copy(annotation = Some(annotation))
        case valid @ Valid(_) =>
          valid // no need to copy as we are only concerned with the invalid results
      }
      if (!resultWithAnnotation.isValid) results.append(resultWithAnnotation)

      index += 1
    }
    if (results.isEmpty) Seq.empty[ValidationResult] else results
  }

  /**
   * Validate an object using @MethodValidation annotated methods
   * @return Failed ValidationResults
   */
  // optimized
  override def validateMethods(obj: Any): Seq[ValidationResult] = {
    val methodValidations: Array[(Method, Annotation)] = findMethodValidations(obj.getClass)

    if (methodValidations.nonEmpty) {
      val results = new ArrayBuffer[ValidationResult]()
      var index = 0
      while (index < methodValidations.length) {
        val (method, annotation) = methodValidations(index)
        val result = method.invoke(obj).asInstanceOf[ValidationResult]
        val resultWithAnnotation = result match {
          case invalid @ Invalid(_, _, _) =>
            invalid.copy(annotation = Some(annotation))
          case valid @ Valid(_) =>
            valid // no need to copy as we are only concerned with the invalid results
        }
        if (!resultWithAnnotation.isValid) results.append(resultWithAnnotation)

        index += 1
      }

      if (results.isEmpty) Seq.empty[ValidationResult] else results
    } else Seq.empty[ValidationResult]
  }

  private[validation] def getValidator[A <: Annotation, V](annotation: A): Validator[A, V] = {
    val validatorClass = getValidatedBy[A, V](annotation.annotationType().asInstanceOf[Class[A]])
    createValidator(validatorClass, annotation)
  }

  private[validation] def isValidationAnnotation(annotation: Annotation): Boolean = {
    annotation.annotationType.isAnnotationPresent(classOf[Validation])
  }

  private[this] def getMethodValidations(clazz: Class[_]): Array[(Method, Annotation)] = {
    for {
      method <- clazz.getMethods
      annotation <- method.getAnnotations
      if isMethodValidationAnnotation(annotation)
    } yield (method, annotation)
  }

  private[this] def isMethodValidationAnnotation(annotation: Annotation): Boolean = {
    annotation.annotationType == classOf[MethodValidation]
  }

  /* Private */

  private[this] def isValid[V](value: V, validator: Validator[_, V]): ValidationResult = {
    value match {
      case _: Option[_] =>
        isValidOption(value.asInstanceOf[Option[V]], validator)
      case _ =>
        validator.isValid(value)
    }
  }

  private[this] def isValidOption[V](
    value: Option[V],
    validator: Validator[_, V]
  ): ValidationResult = {
    value match {
      case Some(actualVal) =>
        validator.isValid(actualVal)
      case _ =>
        Valid()
    }
  }

  private[this] def getValidatedBy[A <: Annotation, V](
    annotationClass: Class[A]
  ): Class[Validator[A, V]] = {
    val validationAnnotation = annotationClass.getAnnotation(classOf[Validation])
    if (validationAnnotation == null)
      throw new IllegalArgumentException("Missing annotation: " + classOf[Validation])
    else
      validationAnnotation.validatedBy().asInstanceOf[Class[Validator[A, V]]]
  }

  private[this] def findValidator[V](annotation: Annotation): Validator[_, V] = {
    validatorMap
      .getOrElseUpdate(annotation, getValidator(annotation))
      .asInstanceOf[Validator[_, V]]
  }

  private[this] def findMethodValidations(clazz: Class[_]): Array[(Method, Annotation)] = {
    methodValidationMap
      .getOrElseUpdate(clazz, getMethodValidations(clazz))
  }

  private[this] def createValidator[A <: Annotation, V](
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
