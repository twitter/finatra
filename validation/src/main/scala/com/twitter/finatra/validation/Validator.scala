package com.twitter.finatra.validation

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.twitter.finatra.utils.ClassUtils
import com.twitter.finatra.validation.ValidationResult.{Invalid, Valid}
import com.twitter.finatra.validation.internal.{
  AnnotatedClass,
  AnnotatedField,
  AnnotatedMethod,
  FieldValidator
}
import com.twitter.inject.conversions.map._
import com.twitter.inject.utils.AnnotationUtils
import com.twitter.util.Try
import java.lang.annotation.Annotation
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import org.json4s.reflect.{ClassDescriptor, ConstructorDescriptor, Reflector}
import scala.collection.{Map, mutable}

object Validator {

  /** The size of the caffeine cache that is used to store reflection data on a validated case class. */
  private val DefaultCacheSize: Long = 128

  /** The Finatra default [[MessageResolver]] which is used to generate error messages when validations fail. */
  private val DefaultMessageResolver: MessageResolver = new MessageResolver

  /**
   * Return a [[Builder]] of a [[Validator]].
   *
   * Users can create a [[Validator]] instance with the builder.
   *
   * For example,
   * {{{
   *   Validator.builder
   *     .withMessageResolver(new CustomizedMessageResolver)
   *     .withCacheSize(512)
   *     .build()
   * }}}
   *
   */
  def builder: Validator.Builder = Builder()

  /**
   * Return a [[Validator]] with Finatra provided default [[MessageResolver]],
   * which will be used to instantiate all [[ConstraintValidator]]s.
   */
  def apply(): Validator = builder.build()

  /**
   * Return a [[Validator]] with a user provided [[MessageResolver]],
   * which will be used to instantiate all [[ConstraintValidator]]s.
   */
  def apply(messageResolver: MessageResolver): Validator =
    builder.withMessageResolver(messageResolver).build()

  /**
   * Used to create a [[Validator]] instance.
   *
   * @param messageResolver When absent, will use [[DefaultMessageResolver]].
   * @param cacheSize When absent, will use [[DefaultCacheSize]].
   */
  case class Builder private[validation] (
    messageResolver: MessageResolver = DefaultMessageResolver,
    cacheSize: Long = DefaultCacheSize) {

    /**
     * Define the [[MessageResolver]] to build a [[Validator]] instance.
     *
     * @note When invoked, the given resolver will be used consistently to build all [[ConstraintValidator]]s.
     *       When invoked multiple times, the last resolver will override all previous ones.
     */
    def withMessageResolver(messageResolver: MessageResolver): Validator.Builder =
      Validator.Builder(messageResolver, this.cacheSize)

    /**
     * Define the size of the cache that stores annotation data for validated case classes.
     */
    def withCacheSize(size: Long): Validator.Builder =
      Validator.Builder(this.messageResolver, size)

    /**
     * @return A [[Validator]] with all attributes defined in the builder.
     */
    def build(): Validator = new Validator(this.cacheSize, this.messageResolver)
  }
}

class Validator private[finatra] (cacheSize: Long, messageResolver: MessageResolver) {

  /** Memoized mapping of [[ConstraintValidator]] to its associated [[Annotation]] class. */
  private[this] val constraintValidatorMap =
    new ConcurrentHashMap[Class[_ <: Annotation], ConstraintValidator[_, _]]()

  // A caffeine cache to store the expensive reflection call result by calling AnnotationUtils.findAnnotations
  // on the same object. Caffeine cache uses the `Window TinyLfu` policy to remove evicted keys.
  // For more information, check out: https://github.com/ben-manes/caffeine/wiki/Efficiency
  private[this] val reflectionCache: Cache[Class[_], AnnotatedClass] =
    Caffeine
      .newBuilder()
      .maximumSize(cacheSize)
      .build[Class[_], AnnotatedClass]()

  /**
   * Validate all field and method annotations for a case class.
   *
   * @throws ValidationException with all invalid validation results.
   * @param obj The case class to validate.
   */
  @throws[ValidationException]
  def validate(obj: Any): Unit = {
    val clazz: Class[_] = obj.getClass
    if (!ClassUtils.maybeIsCaseClass(clazz)) throw InvalidCaseClassException(clazz)

    val AnnotatedClass(_, fields, methods) = getAnnotatedClass(clazz)

    // validate field
    val fieldValidations: Iterable[ValidationResult] =
      fields.flatMap {
        case (_, AnnotatedField(Some(field), fieldValidators)) =>
          field.setAccessible(true)
          validateField(field.get(obj), fieldValidators)
      }

    val invalidResult = fieldValidations.toSeq ++ validateMethods(obj, methods) // validate methods
    if (invalidResult.nonEmpty) throw new ValidationException(invalidResult)
  }

  /**
   * Validate a field's value according to the field's constraint annotations.
   *
   * @return A list of all the failed [[ValidationResult]].
   */
  // note: Prefer while loop over Scala for loop for better performance. The scala for loop
  // performance is optimized in 2.13.0 if we enable scalac: https://github.com/scala/bug/issues/1338
  private[finatra] def validateField[T](
    fieldValue: T,
    fieldValidators: Array[FieldValidator]
  ): Seq[ValidationResult] = {
    if (fieldValidators.nonEmpty) {
      val results = new mutable.ArrayBuffer[ValidationResult](fieldValidators.length)
      var index = 0
      while (index < fieldValidators.length) {
        val FieldValidator(constraintValidator, annotation) = fieldValidators(index)
        val result = isValid(
          fieldValue,
          annotation,
          constraintValidator.asInstanceOf[ConstraintValidator[Annotation, T]])
        val resultWithAnnotation = parseResult(result, annotation)
        if (!resultWithAnnotation.isValid) results.append(resultWithAnnotation)
        index += 1
      }
      results
    } else Seq.empty[ValidationResult]
  }

  /**
   * Validate all methods annotated with [[MethodValidation]] in a case class.
   *
   * @return A list of all the failed [[ValidationResult]].
   */
  // note: Prefer while loop over Scala for loop for better performance. The scala for loop
  // performance is optimized in 2.13.0 if we enable scalac: https://github.com/scala/bug/issues/1338
  private[finatra] def validateMethods(
    obj: Any,
    methods: Array[AnnotatedMethod]
  ): Seq[ValidationResult] = {
    if (methods.nonEmpty) {
      val results = new mutable.ArrayBuffer[ValidationResult](methods.length)
      var index = 0
      while (index < methods.length) {
        val AnnotatedMethod(method, annotation) = methods(index)
        val result = method.invoke(obj).asInstanceOf[ValidationResult]
        val resultWithAnnotation = parseResult(result, annotation)
        if (!resultWithAnnotation.isValid) results.append(resultWithAnnotation)
        index += 1
      }
      results
    } else Seq.empty[ValidationResult]
  }

  @deprecated(
    "The endpoint is not supposed to be use on its own, use validate(Any) instead",
    "2020-02-12")
  def validateMethods(obj: Any): Seq[ValidationResult] = {
    val methodValidations: Array[AnnotatedMethod] = getMethodValidations(obj.getClass)
    validateMethods(obj, methodValidations)
  }

  /* --------------------------------------------------------------------------*
   *                                   Utils
   * --------------------------------------------------------------------------*/

  // Exposed for finatra/jackson
  // Look for the ConstraintValidator for a given annotation
  private[finatra] def findFieldValidator[T](annotation: Annotation): FieldValidator = {
    val constraintValidator =
      constraintValidatorMap
        .atomicGetOrElseUpdate(annotation.getClass, getConstraintValidator(annotation))
        .asInstanceOf[ConstraintValidator[_ <: Annotation, T]]
    FieldValidator(constraintValidator, annotation)
  }

  // Exposed for finatra/jackson
  private[finatra] def isMethodValidationAnnotation(annotation: Annotation): Boolean =
    AnnotationUtils.annotationEquals[MethodValidation](annotation)

  // Exposed for finatra/jackson
  private[finatra] def isConstraintAnnotation(annotation: Annotation): Boolean =
    AnnotationUtils.isAnnotationPresent[Constraint](annotation)

  // Exposed for finatra/jackson and testing
  private[finatra] def getMethodValidations(clazz: Class[_]): Array[AnnotatedMethod] =
    for {
      method <- clazz.getMethods
      annotation = Try(method.getAnnotation(classOf[MethodValidation]))
      if annotation.isReturn && annotation.get() != null
    } yield AnnotatedMethod(method, annotation.get())

  // Exposed in testing in finatra/http integration test
  // Return all field constraint annotations and their matching ConstraintValidators
  // and all method annotations.
  private[finatra] def getAnnotatedClass(clazz: Class[_]): AnnotatedClass = {
    reflectionCache.get(
      clazz,
      new Function[Class[_], AnnotatedClass] {
        def apply(v1: Class[_]): AnnotatedClass = {
          val clazzDescriptor: ClassDescriptor =
            Reflector.describe(clazz).asInstanceOf[ClassDescriptor]
          // We must use the constructor parameters here (and not getDeclaredFields),
          // because getDeclaredFields will return other non-constructor fields within
          // the case class. We use `head` here, because if this case class doesn't have a
          // constructor at all, we have larger issues.
          // Note: we do not use clazz.constructors.head.getParameters because access to parameter
          // names via Java reflection is not supported in Scala 2.11
          // see: https://github.com/scala/bug/issues/9437
          val constructor: ConstructorDescriptor = clazzDescriptor.constructors.head
          createAnnotatedClass(
            clazz,
            AnnotationUtils.findAnnotations(
              clazz,
              constructor.params.map(_.argType.erasure),
              constructor.params.map(_.name).toArray))
        }
      }
    )
  }

  private[this] def parseResult(
    result: ValidationResult,
    annotation: Annotation
  ): ValidationResult = {
    result match {
      case invalid @ Invalid(_, _, _) =>
        invalid.copy(annotation = Some(annotation))
      case valid @ Valid(_) =>
        valid // no need to copy as we are only concerned with the invalid results
    }
  }

  // Exposed in finatra/jackson
  private[finatra] def createAnnotatedClass(
    clazz: Class[_],
    annotationsMap: Map[String, Array[Annotation]]
  ): AnnotatedClass = {
    val fieldsMap = new mutable.HashMap[String, AnnotatedField]()
    // get AnnotatedField
    for ((name, annotations) <- annotationsMap) yield {
      for (i <- annotations.indices) {
        val annotation = annotations(i)
        if (isConstraintAnnotation(annotation)) {
          val fieldValidators = fieldsMap.get(name) match {
            case Some(validators) =>
              validators.fieldValidators :+ findFieldValidator(annotation)
            case _ =>
              Array(findFieldValidator(annotation))
          }
          fieldsMap.put(
            name,
            AnnotatedField(
              // the annotation may be from a static or secondary constructor for which we have no field information
              Try(clazz.getDeclaredField(name)).toOption,
              fieldValidators
            )
          )
        }
      }
    }
    AnnotatedClass(clazz, fieldsMap.toMap, getMethodValidations(clazz))
  }

  private[this] def isValid[V](
    value: V,
    annotation: Annotation,
    validator: ConstraintValidator[Annotation, V]
  ): ValidationResult =
    value match {
      case _: Option[_] =>
        isValidOption(value.asInstanceOf[Option[V]], annotation, validator)
      case _ =>
        validator.isValid(annotation, value)
    }

  private[this] def isValidOption[V](
    value: Option[V],
    annotation: Annotation,
    validator: ConstraintValidator[Annotation, V]
  ): ValidationResult =
    value match {
      case Some(actualVal) =>
        validator.isValid(annotation, actualVal)
      case _ =>
        Valid()
    }

  // Each Constraint annotation defines its ConstraintValidator in a `validatedBy` field
  private[this] def getValidatedBy[A <: Annotation, T](
    annotationClass: Class[A]
  ): Class[ConstraintValidator[A, T]] = {
    val validationAnnotation = annotationClass.getAnnotation(classOf[Constraint])
    if (validationAnnotation == null)
      throw new IllegalArgumentException("Missing annotation: " + classOf[Constraint])
    else
      validationAnnotation.validatedBy().asInstanceOf[Class[ConstraintValidator[A, T]]]
  }

  private[this] def getConstraintValidator[A <: Annotation, T](
    annotation: A
  ): ConstraintValidator[A, T] = {
    val validatorClass = getValidatedBy[A, T](annotation.annotationType().asInstanceOf[Class[A]])
    createConstraintValidator(validatorClass, annotation)
  }

  private[this] def createConstraintValidator[A <: Annotation, T](
    validatorClass: Class[ConstraintValidator[A, T]],
    annotation: A
  ): ConstraintValidator[A, T] = {
    try {
      validatorClass
        .getConstructor(classOf[MessageResolver])
        .newInstance(messageResolver)
    } catch {
      case _: NoSuchMethodException =>
        throw new IllegalArgumentException(
          "Validator [%s] does not contain a constructor with parameter type: [%s]"
            .format(validatorClass, messageResolver.getClass)
        )
    }
  }
}
