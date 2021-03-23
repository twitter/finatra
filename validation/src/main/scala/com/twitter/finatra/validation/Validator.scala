package com.twitter.finatra.validation

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.twitter.finatra.validation.internal._
import com.twitter.inject.conversions.map._
import com.twitter.util.logging.Logger
import com.twitter.util.reflect.Annotations.findDeclaredAnnotations
import com.twitter.util.reflect.{Annotations, Classes, Types}
import com.twitter.util.{Return, Try}
import java.lang.annotation.Annotation
import java.lang.reflect.{Field, Parameter}
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import org.json4s.reflect.{ClassDescriptor, ConstructorDescriptor, Reflector, ScalaType}
import scala.collection.{Map, mutable}
import scala.util.control.NonFatal

object Validator {
  private[this] val logger = Logger.getLogger(Validator.getClass)

  /** The size of the caffeine cache that is used to store reflection data on a validated case class. */
  private val DefaultCacheSize: Long = 128

  /** The Finatra default [[MessageResolver]] which is used to generate error messages when validations fail. */
  private val DefaultMessageResolver: MessageResolver = new MessageResolver

  /* Exposed for finatra/jackson */
  private[finatra] def parseResult(
    result: ValidationResult,
    path: Path,
    annotation: Annotation
  ): ValidationResult = {
    result match {
      case invalid @ ValidationResult.Invalid(_, _, _, _) =>
        invalid.copy(path = path, annotation = Some(annotation))
      case _ => result // no need to copy as we are only concerned with the invalid results
    }
  }

  /* Exposed for finatra/jackson */
  private[finatra] def isMethodValidationAnnotation(annotation: Annotation): Boolean =
    Annotations.annotationEquals[MethodValidation](annotation)

  /* Exposed for finatra/jackson */
  private[finatra] def isConstraintAnnotation(annotation: Annotation): Boolean =
    Annotations.isAnnotationPresent[Constraint](annotation)

  /* Exposed for finatra/jackson */
  private[finatra] def extractFieldsFromMethodValidation(
    annotation: Option[Annotation]
  ): Iterable[String] = {
    annotation match {
      case Some(methodValidation: MethodValidation) =>
        methodValidation.fields.toIterable.filter(_.nonEmpty)
      case _ =>
        Iterable.empty[String]
    }
  }

  // note: Prefer while loop over Scala for loop for better performance. The scala for loop
  // performance is optimized in 2.13.0 if we enable scalac: https://github.com/scala/bug/issues/1338
  /* Exposed for finatra/jackson */
  private[finatra] def validateMethods(
    obj: Any,
    methods: Array[AnnotatedMethod]
  )(
    invalidResultFn: (Path, ValidationResult.Invalid) => Unit
  ): Unit = {
    if (methods.nonEmpty) {
      var index = 0
      while (index < methods.length) {
        val AnnotatedMethod(_, path, method, annotation) = methods(index)
        // if we're unable to invoke the method, we want this propagate
        val result = method.invoke(obj).asInstanceOf[ValidationResult]
        val resultWithAnnotation = parseResult(result, path, annotation)
        if (!resultWithAnnotation.isValid) {
          invalidResultFn(path, resultWithAnnotation.asInstanceOf[ValidationResult.Invalid])
        }
        index += 1
      }
    }
  }

  /* Exposed for finatra/jackson */
  private[finatra] def getMethodValidations(clazz: Class[_]): Array[AnnotatedMethod] =
    getMethodValidations(clazz, None)

  // method validations with an optional parent Path
  private def getMethodValidations(
    clazz: Class[_],
    parentPath: Option[Path]
  ): Array[AnnotatedMethod] = {
    for {
      method <- clazz.getMethods
      annotation = Try(method.getAnnotation(classOf[MethodValidation]))
      if annotation.isReturn && annotation.get() != null
    } yield {
      AnnotatedMethod(
        name = Some(method.getName),
        path = parentPath match {
          case Some(p) =>
            p.append(method.getName)
          case _ =>
            Path(method.getName)
        },
        method = method,
        annotation = annotation.get()
      )
    }
  }

  /** If the given class is marked for cascaded validation or not. True if the class is a case class and has the `@Valid` annotation */
  private def isCascadedValidation(erasure: Class[_], annotations: Array[Annotation]): Boolean =
    Types.isCaseClass(erasure) && Annotations.findAnnotation[Valid](annotations).isDefined

  /** Each Constraint annotation defines its ConstraintValidator in a `validatedBy` field */
  private def getValidatedBy[A <: Annotation, V](
    annotationClass: Class[A]
  ): Class[ConstraintValidator[A, V]] = {
    val validationAnnotation = annotationClass.getAnnotation(classOf[Constraint])
    if (validationAnnotation == null)
      throw new IllegalArgumentException("Missing annotation: " + classOf[Constraint])
    else
      validationAnnotation.validatedBy().asInstanceOf[Class[ConstraintValidator[A, V]]]
  }

  private def isValidOption[V](
    value: Option[V],
    annotation: Annotation,
    validator: ConstraintValidator[Annotation, V]
  ): ValidationResult =
    value match {
      case Some(actualVal) =>
        validator.isValid(annotation, actualVal)
      case _ =>
        ValidationResult.Valid()
    }

  // this currently does not work properly for constraints which support Options
  private def isValid[V](
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

  // note: Prefer while loop over Scala for loop for better performance. The scala for loop
  // performance is optimized in 2.13.0 if we enable scalac: https://github.com/scala/bug/issues/1338
  private def validateField[T](
    fieldValue: T,
    path: Path,
    fieldValidators: Array[FieldValidator]
  ): mutable.ListBuffer[ValidationResult] = {
    if (fieldValidators.nonEmpty) {
      val results = new mutable.ListBuffer[ValidationResult]()
      var index = 0
      while (index < fieldValidators.length) {
        val FieldValidator(constraintValidator, annotation) = fieldValidators(index)
        val result =
          try {
            isValid(
              fieldValue,
              annotation,
              constraintValidator.asInstanceOf[ConstraintValidator[Annotation, T]])
          } catch {
            case NonFatal(e) =>
              logger.error("Unexpected exception validating field: " + path.toString, e)
              throw e
          }
        val resultWithAnnotation = parseResult(result, path, annotation)
        if (!resultWithAnnotation.isValid) results.append(resultWithAnnotation)
        index += 1
      }
      results
    } else mutable.ListBuffer.empty[ValidationResult]
  }

  // find the constructor with parameters that are all declared class fields
  // we use json4s here because in Scala 2.11 constructor params do not carry their
  // actual names (fixed in 2.12+) and we have logic based on the name of the constructor param
  private def defaultConstructor(clazzDescriptor: ClassDescriptor): ConstructorDescriptor = {
    def isDefaultConstructorDescriptor(constructorDescriptor: ConstructorDescriptor): Boolean = {
      constructorDescriptor.params.forall { param =>
        Try(clazzDescriptor.erasure.erasure.getDeclaredField(param.name)).isReturn
      }
    }

    clazzDescriptor.constructors
      .find(isDefaultConstructorDescriptor)
      .orElse(clazzDescriptor.constructors.find(_.params.isEmpty))
      .getOrElse(throw new IllegalArgumentException(
        s"Unable to parse case class for validation: ${clazzDescriptor.erasure.fullName}"))
  }

  /** Returns a Parameter name to AnnotatedConstructorParamDescriptor map */
  private def getAnnotatedConstructorParamDescriptors(
    clazz: Class[_],
    clazzDescriptor: ClassDescriptor
  ): scala.collection.Map[String, AnnotatedConstructorParamDescriptor] = {
    // find the default constructor descriptor via json4s.
    val constructorDescriptor: ConstructorDescriptor = defaultConstructor(clazzDescriptor)
    // attempt to locate the class constructor with the same param args
    val constructor = Try(
      clazz.getConstructor(constructorDescriptor.params.map(_.argType.erasure): _*))
    // if we can find a constructor get the reflection Parameters which carry the annotations
    val constructorParameters: Array[Parameter] = constructor match {
      case Return(cons) =>
        cons.getParameters
      case _ =>
        Array.empty
    }

    // find all inherited annotations for every constructor param
    val allFieldAnnotations = findAnnotations(
      clazz,
      clazz.getDeclaredFields.map(_.getName).toSet,
      constructorDescriptor.params.map { param =>
        param.name -> constructorParameters(param.argIndex).getAnnotations
      }.toMap
    )

    val result: mutable.HashMap[String, AnnotatedConstructorParamDescriptor] =
      new mutable.HashMap[String, AnnotatedConstructorParamDescriptor]()
    var index = 0
    while (index < constructorParameters.length) {
      val parameter = constructorParameters(index)
      val descriptor = constructorDescriptor.params(index)
      val filteredAnnotations = allFieldAnnotations(descriptor.name).filter { ann =>
        Annotations.isAnnotationPresent[Constraint](ann) ||
        Annotations.annotationEquals[Valid](ann)
      }

      result.put(
        descriptor.name,
        AnnotatedConstructorParamDescriptor(
          descriptor,
          Types.parameterizedTypeNames(parameter.getParameterizedType),
          filteredAnnotations
        )
      )

      index += 1
    }

    result
  }

  private[this] def findAnnotations(
    clazz: Class[_],
    declaredFields: Set[String],
    fieldAnnotations: scala.collection.Map[String, Array[Annotation]]
  ): scala.collection.Map[String, Array[Annotation]] = {
    val collectorMap = new scala.collection.mutable.HashMap[String, Array[Annotation]]()
    collectorMap ++= fieldAnnotations
    // find inherited annotations
    findDeclaredAnnotations(
      clazz,
      declaredFields,
      collectorMap
    )
  }

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
  import Validator._

  /** Memoized mapping of [[ConstraintValidator]] to its associated [[Annotation]] class. */
  private[this] val constraintValidatorMap =
    new ConcurrentHashMap[Class[_ <: Annotation], ConstraintValidator[_, _]]()

  // A caffeine cache to store the expensive reflection calls on the same object. Caffeine cache
  // uses the `Window TinyLfu` policy to remove evicted keys.
  // For more information, check out: https://github.com/ben-manes/caffeine/wiki/Efficiency
  private[this] val reflectionCache: Cache[Class[_], AnnotatedClass] =
    Caffeine
      .newBuilder()
      .maximumSize(cacheSize)
      .build[Class[_], AnnotatedClass]()

  /* Public */

  /**
   * Validates all constraints on given object which is expected to be an
   * instance of a Scala [[https://docs.scala-lang.org/tour/case-classes.html case class]].
   *
   * @throws ValidationException with all invalid validation results.
   * @param obj The case class to validate.
   *
   * @note the [[ValidationException]] formats included [[ValidationResult.Invalid]] results to
   *       include the [[ValidationResult.Invalid.path]] as a [[ValidationResult.Invalid.message]]
   *       prefix for ease of error reporting from the exception as the results are used as the basis
   *       of the [[ValidationException.getMessage]] string.
   */
  @throws[ValidationException]
  def verify(obj: Any): Unit = {
    // validate fields
    val invalidResult = validate(obj)
    if (invalidResult.nonEmpty)
      throw new ValidationException(includeFieldNames = true, invalidResult)
  }

  /**
   * Validates all constraints on given object which is expected to be an
   * instance of a Scala [[https://docs.scala-lang.org/tour/case-classes.html case class]].
   *
   * @param obj The case class to validate.
   * @return a sequence of [[ValidationResult]] which represent all found constraint violations.
   *
   * @note [[ValidationResult.Invalid]] results returned from this method will not have the
   *       field name already included in the message. That translations happens in the [[ValidationException]].
   */
  def validate(obj: Any): Set[ValidationResult] = {
    val results = validate(obj, None)
    if (results.nonEmpty) results.toSet
    else Set.empty[ValidationResult]
  }

  /* Private */

  /* Exposed for finatra/jackson */
  private[finatra] def validateField[T](
    fieldValue: T,
    fieldName: String,
    fieldValidators: Array[FieldValidator]
  ): mutable.ListBuffer[ValidationResult] =
    Validator.validateField(fieldValue, Path(fieldName), fieldValidators)

  // recursively validate
  private[this] def validate(
    value: Any,
    annotatedMember: Option[AnnotatedMember],
    shouldFindFieldValue: Boolean = true
  ): Iterable[ValidationResult] =
    annotatedMember match {
      case Some(member) =>
        member match {
          case AnnotatedField(_, path, _, fieldValidators) =>
            Validator.validateField(value, path, fieldValidators)
          case AnnotatedClass(_, _, _, _, members, methods) =>
            val fieldValidationResults = members.flatMap {
              case collection: AnnotatedClass
                  if collection.scalaType.isDefined && collection.scalaType.get.isCollection =>
                validateCollection(
                  findFieldValue(value, value.getClass, collection.name),
                  collection)
              case option: AnnotatedClass
                  if option.scalaType.isDefined && option.scalaType.get.isOption =>
                validateOption(findFieldValue(value, value.getClass, option.name), option)
              case annotatedClass: AnnotatedClass =>
                if (shouldFindFieldValue)
                  validate(
                    findFieldValue(value, value.getClass, annotatedClass.name),
                    Some(annotatedClass))
                else
                  validate(value, Some(annotatedClass))
              case annotatedField: AnnotatedField =>
                // otherwise need to find the correct obj
                validate(
                  findFieldValue(value, value.getClass, annotatedField.name),
                  Some(annotatedField))
            }
            fieldValidationResults ++ validateMethods(value, methods)
          case _ => // make exhaustive
            Iterable.empty[ValidationResult]
        }
      case _ =>
        val clazz: Class[_] = value.getClass
        if (Types.notCaseClass(clazz)) throw InvalidCaseClassException(clazz)
        val annotatedClazz: AnnotatedClass = findAnnotatedClass(clazz)
        validate(value, Some(annotatedClazz))
    }

  private[this] def findFieldValue[T](
    obj: Any,
    clazz: Class[_],
    fieldName: Option[String]
  ): T =
    fieldName
      .map { name =>
        val field = clazz.getDeclaredField(name)
        field.setAccessible(true)
        field.get(obj).asInstanceOf[T]
      }.getOrElse(obj.asInstanceOf[T])

  private[this] def validateCollection(
    obj: Iterable[_],
    annotatedClass: AnnotatedClass
  ): Iterable[ValidationResult] = {
    // apply the parentPropertyPath to the contained members for error reporting
    def indexAnnotatedMembers(
      parentPropertyPath: Path,
      annotatedMember: AnnotatedMember
    ): Array[AnnotatedMember] = annotatedMember match {
      case annotatedClass: AnnotatedClass =>
        annotatedClass.members.map {
          case f: AnnotatedField =>
            f.copy(path = Path(parentPropertyPath.names :+ f.path.last))
          case m: AnnotatedMethod =>
            m.copy(path = Path(parentPropertyPath.names :+ m.path.last))
          case c: AnnotatedClass =>
            val newParentPath = Path(parentPropertyPath.names :+ c.path.last)
            c.copy(
              path = newParentPath,
              members = indexAnnotatedMembers(newParentPath, c),
              methods = c.methods.map { m =>
                m.copy(path = Path(parentPropertyPath.names :+ m.path.last))
              }
            )
        }
      case _ =>
        throw new IllegalArgumentException(
          s"Unrecognized AnnotatedMember type: ${annotatedMember.getClass}")
    }

    (for ((instance, index) <- obj.zipWithIndex) yield {
      // apply the index to the parent path, then use this to recompute paths of members and methods
      val parentPropertyPath = annotatedClass.path.append(index.toString)
      validate(
        instance,
        Some(
          annotatedClass
            .copy(
              scalaType = annotatedClass.scalaType.map(_.typeArgs.head),
              path = parentPropertyPath,
              members = indexAnnotatedMembers(parentPropertyPath, annotatedClass),
              methods = annotatedClass.methods.map { m =>
                m.copy(path = Path(parentPropertyPath.names :+ m.path.last))
              }
            )
        ),
        // if the type to validate is a case class we need to use the zipped instance here and
        // find the field value for the case class members otherwise use the already calculated
        // given instance. thus shouldFindFieldValue is true if the given annotatedClass has
        // a case class type argument
        shouldFindFieldValue =
          annotatedClass.scalaType.exists(t => Types.isCaseClass(t.typeArgs.head.erasure))
      )
    }).flatten
  }

  // performs validation based on the first typeArg of the argType of the given AnnotatedClass
  private[this] def validateOption(
    obj: Option[_],
    annotatedClass: AnnotatedClass
  ): Iterable[ValidationResult] = {
    obj match {
      case Some(instance) =>
        validate(
          instance,
          Some(
            annotatedClass
              .copy(scalaType = annotatedClass.scalaType.map(_.typeArgs.head))),
          shouldFindFieldValue = false // we've already calculated it here
        )
      case _ => Iterable.empty
    }
  }

  /* Exposed for testing */
  private[validation] def validateMethods(
    obj: Any,
    methods: Array[AnnotatedMethod]
  ): mutable.HashSet[ValidationResult] = {
    val results = new mutable.HashSet[ValidationResult]()
    Validator.validateMethods(obj, methods) {
      case (path, invalid) =>
        val caseClassFields = extractFieldsFromMethodValidation(invalid.annotation)
        // in the Jackson case, method validations are repeated per-field listed in the
        // @MethodValidation annotation. here we recreate the behavior for parity
        // we want to create a new path per field reported in the MethodValidation
        val failures = caseClassFields
        // per field, create a new Path with the given field as the leaf
          .map(fieldName => path.copy(names = path.init :+ fieldName))
          .map(p =>
            invalid.copy(path = p)) // create a new ValidationResult.Invalid with the given Path
        if (failures.nonEmpty) failures.map(results.add)
        else results.add(invalid)
    }
    results
  }

  /* Derive AnnotatedClass for Validation */

  /* Exposed for testing */
  private[validation] def findAnnotatedClass(clazz: Class[_]): AnnotatedClass =
    reflectionCache.get(
      clazz,
      new Function[Class[_], AnnotatedClass] {
        def apply(v1: Class[_]): AnnotatedClass = {
          getAnnotatedClass(None, Path.Empty, clazz, None, Seq.empty[Class[_]])
        }
      }
    )

  // recursively describe a class
  private def getAnnotatedClass(
    name: Option[String],
    path: Path,
    clazz: Class[_],
    scalaType: Option[ScalaType],
    observed: Seq[Class[_]]
  ): AnnotatedClass = {
    // for case classes, annotations for params only appear on the constructor
    val clazzDescriptor: ClassDescriptor = Reflector.describe(clazz).asInstanceOf[ClassDescriptor]
    // map of parameter name to a class with all annotations for the parameter (including inherited)
    val annotatedConstructorParams: Map[String, AnnotatedConstructorParamDescriptor] =
      getAnnotatedConstructorParamDescriptors(clazz, clazzDescriptor)

    val members = new mutable.ArrayBuffer[AnnotatedMember]()
    // we validate only declared fields of the class
    val declaredFields: Array[Field] = clazz.getDeclaredFields
    var index = 0
    while (index < declaredFields.length) {
      val field = declaredFields(index)
      val (scalaType, fieldClazz, annotations) =
        annotatedConstructorParams.get(field.getName) match {
          case Some(annotatedParam) =>
            // we already have information for the field as it is a constructor parameter
            (
              Some(annotatedParam.param.argType),
              annotatedParam.param.argType.erasure,
              annotatedParam.annotations)
          case _ =>
            // we don't have information, use the field information which should carry annotation information
            (None, field.getType, field.getAnnotations)
        }

      // create an annotated field
      getAnnotatedField(field.getName, path.append(field.getName), Some(field), annotations)
        .foreach { annotatedField =>
          members.append(annotatedField)
        }
      // create an annotated class
      getAnnotatedClass(field.getName, path, fieldClazz, scalaType, annotations, observed :+ clazz)
        .foreach { annotatedClazz =>
          members.append(annotatedClazz)
        }

      index += 1
    }

    AnnotatedClass(
      name,
      path,
      clazz,
      scalaType.orElse(Some(clazzDescriptor.erasure)),
      members.toArray,
      getMethodValidations(clazz, Some(path))
    )
  }

  private[this] def createConstraintValidator[A <: Annotation, T](
    validatorClass: Class[ConstraintValidator[A, T]]
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

  private[this] def getConstraintValidator[A <: Annotation, V](
    annotation: A
  ): ConstraintValidator[A, V] = {
    val validatorClass =
      getValidatedBy[A, V](annotation.annotationType().asInstanceOf[Class[A]])
    createConstraintValidator(validatorClass)
  }

  /* Exposed for finatra/jackson */
  private[finatra] def findFieldValidator[V](annotation: Annotation): FieldValidator = {
    val constraintValidator =
      constraintValidatorMap
        .atomicGetOrElseUpdate(annotation.getClass, getConstraintValidator(annotation))
        .asInstanceOf[ConstraintValidator[_ <: Annotation, V]]
    FieldValidator(constraintValidator, annotation)
  }

  /** Optionally create an AnnotatedField is the field is annotated with any Constraints */
  private[this] def getAnnotatedField(
    name: String,
    path: Path,
    field: Option[Field],
    annotations: Array[Annotation]
  ): Option[AnnotatedField] = {

    val fieldValidators = new mutable.ArrayBuffer[FieldValidator](annotations.length)
    var index = 0
    while (index < annotations.length) {
      val annotation = annotations(index)
      if (isConstraintAnnotation(annotation)) {
        fieldValidators.append(findFieldValidator(annotation))
      }
      index += 1
    }
    if (fieldValidators.nonEmpty) {
      Some(
        AnnotatedField(
          name = Some(name),
          path = path,
          field = field,
          fieldValidators = fieldValidators.toArray
        )
      )
    } else None
  }

  /** Optionally create an AnnotatedClass if the class is a Scala case class and is annotated with `@Valid` */
  private def getAnnotatedClass(
    name: String,
    path: Path,
    clazz: Class[_],
    scalaType: Option[ScalaType],
    annotations: Array[Annotation],
    observed: Seq[Class[_]]
  ): Option[AnnotatedClass] = {
    // note we do not handle validation of container types with multiple
    // case class type args, e.g., Map, Either, Tuple2, etc.
    val argTypeOption = scalaType.orElse {
      Reflector.describe(clazz) match {
        case desc: ClassDescriptor =>
          Some(desc.erasure)
        case _ => None
      }
    }
    argTypeOption match {
      case Some(argType) if isCascadedValidation(argType.erasure, annotations) =>
        if (observed.contains(argType.erasure))
          throw new IllegalArgumentException(s"Cycle detected at ${argType.erasure}.")
        Some(
          getAnnotatedClass(
            Some(name),
            path.append(name),
            argType.erasure,
            Some(argType),
            observed :+ argType.erasure))
      case Some(argType) if (argType.isMap || argType.isMutableMap) =>
        // Maps not supported
        None
      case Some(argType)
          if (argType.isCollection || argType.isOption) &&
            isCascadedValidation(argType.typeArgs.head.erasure, annotations) =>
        // handles Option and Iterable collections
        if (observed.contains(argType.typeArgs.head.erasure))
          throw new IllegalArgumentException(s"Cycle detected at ${argType.typeArgs.head.erasure}.")
        Some(
          getAnnotatedClass(
            Some(name),
            path
              .append(name)
              .append(Classes.simpleName(argType.typeArgs.head.erasure).toLowerCase),
            argType.typeArgs.head.erasure,
            Some(argType),
            observed :+ argType.typeArgs.head.erasure
          ))
      case _ => None
    }
  }
}
