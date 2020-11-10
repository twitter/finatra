package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.annotation.{JsonCreator, JsonIgnoreProperties}
import com.fasterxml.jackson.core.{
  JsonFactory,
  JsonParseException,
  JsonParser,
  JsonProcessingException,
  JsonToken
}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.exc.{
  InvalidDefinitionException,
  InvalidFormatException,
  MismatchedInputException,
  UnrecognizedPropertyException
}
import com.fasterxml.jackson.databind.introspect._
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition
import com.twitter.finatra.jackson.caseclass.exceptions.{
  CaseClassFieldMappingException,
  CaseClassMappingException,
  ErrorCode,
  InjectableValuesException,
  _
}
import com.twitter.finatra.json.annotations.JsonCamelCase
import com.twitter.finatra.validation.ValidationResult.Invalid
import com.twitter.finatra.validation.internal.{
  FieldValidator,
  AnnotatedField => ValidationAnnotatedField,
  AnnotatedMember => ValidationAnnotatedMember,
  AnnotatedMethod => ValidationAnnotatedMethod
}
import com.twitter.finatra.validation.{
  Path,
  ValidationResult,
  Validator,
  ErrorCode => ValidationErrorCode
}
import com.twitter.inject.{Logging, TypeUtils}
import com.twitter.inject.domain.WrappedValue
import com.twitter.inject.utils.AnnotationUtils
import com.twitter.util.Try
import java.lang.annotation.Annotation
import java.lang.reflect.{
  Constructor,
  Executable,
  InvocationTargetException,
  Method,
  Parameter,
  ParameterizedType,
  Type,
  TypeVariable
}
import javax.annotation.concurrent.ThreadSafe
import org.json4s.reflect.{ClassDescriptor, ConstructorDescriptor, Reflector, ScalaType}
import scala.collection.JavaConverters._
import scala.collection.{Map, mutable}
import scala.util.control.NonFatal

private object CaseClassDeserializer {
  // For reporting an InvalidDefinitionException
  val EmptyJsonParser: JsonParser = new JsonFactory().createParser("")

  /* For supporting JsonCreator */
  case class CaseClassCreator(
    executable: Executable,
    propertyDefinitions: Array[PropertyDefinition])

  /* Holder for a fully specified JavaType with generics and a Jackson BeanPropertyDefinition */
  case class PropertyDefinition(
    javaType: JavaType,
    scalaType: ScalaType,
    beanPropertyDefinition: BeanPropertyDefinition)

  /* Holder of constructor arg to ScalaType */
  case class ConstructorParam(name: String, scalaType: ScalaType)

  case class ValidationDescriptor(
    clazz: Class[_],
    path: Path,
    fields: Map[String, ValidationAnnotatedField],
    methods: Array[ValidationAnnotatedMethod])
      extends ValidationAnnotatedMember {
    // the name is tracked by the deserializer
    override final val name: Option[String] = None
  }
}

/**
 * Custom case class deserializer which overcomes limitations in jackson-scala-module.
 *
 * Our improvements:
 * - Throws a [[JsonMappingException]] when non-`Option` fields are missing in the incoming JSON.
 * - Does not allow for case class constructor fields to ever be constructed with a `null` reference.
 *    Any field which is marked to not be read from the incoming JSON must either be injected with a
 *    configured InjectableValues implementation or have a default value which can be supplied when
 *    the deserializer constructs the case class. Otherwise an error will be returned.
 * - Use default values when fields are missing in the incoming JSON.
 * - Properly deserialize a `Seq[Long]` (see https://github.com/FasterXML/jackson-module-scala/issues/62)
 * - Support "wrapped values" using [[WrappedValue]].
 * - Support for field and method level validations.
 *
 * The following Jackson annotations which affect deserialization are not explicitly supported by
 * this deserializer (note this list may not be exhaustive):
 * - @JsonPOJOBuilder
 * - @JsonAlias
 * - @JsonSetter
 * - @JsonAnySetter
 * - @JsonTypeName
 * - @JsonUnwrapped
 * - @JsonManagedReference
 * - @JsonBackReference
 * - @JsonIdentityInfo
 * - @JsonTypeIdResolver
 *
 * For many, this is because the behavior of these annotations is ambiguous when it comes to application on
 * constructor arguments of a Scala case class during deserialization.
 *
 * @see [[https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations]]
 *
 * @note This class is inspired by Jerkson's CaseClassDeserializer which can be found here:
 *       [[https://github.com/codahale/jerkson/blob/master/src/main/scala/com/codahale/jerkson/deser/CaseClassDeserializer.scala]]
 */
/* exposed for testing */
@ThreadSafe
private[jackson] class CaseClassDeserializer(
  javaType: JavaType,
  config: DeserializationConfig,
  beanDescription: BeanDescription,
  injectableTypes: InjectableTypes,
  validator: Option[Validator])
    extends JsonDeserializer[AnyRef]
    with Logging {
  import CaseClassDeserializer._

  private[this] val clazz: Class[_] = javaType.getRawClass
  // we explicitly do not read a mix-in for a primitive type
  private[this] val mixinClazz: Option[Class[_]] =
    Option(config.findMixInClassFor(clazz))
      .flatMap(m => if (m.isPrimitive) None else Some(m))
  private[this] val clazzDescriptor: ClassDescriptor =
    Reflector.describe(clazz).asInstanceOf[ClassDescriptor]
  private[this] val clazzAnnotations: Array[Annotation] =
    mixinClazz.map(_.getAnnotations) match {
      case Some(mixinAnnotations) =>
        clazz.getAnnotations ++ mixinAnnotations
      case _ =>
        clazz.getAnnotations
    }

  private[this] val caseClazzCreator: CaseClassCreator = {
    val fromCompanion: Option[AnnotatedMethod] =
      beanDescription.getFactoryMethods.asScala.find(_.hasAnnotation(classOf[JsonCreator]))
    val fromClazz: Option[AnnotatedConstructor] =
      beanDescription.getConstructors.asScala.find(_.hasAnnotation(classOf[JsonCreator]))

    fromCompanion match {
      case Some(jsonCreatorAnnotatedMethod) =>
        CaseClassCreator(
          jsonCreatorAnnotatedMethod.getAnnotated,
          getBeanPropertyDefinitions(
            jsonCreatorAnnotatedMethod.getAnnotated.getParameters,
            jsonCreatorAnnotatedMethod,
            fromCompanion = true)
        )
      case _ =>
        fromClazz match {
          case Some(jsonCreatorAnnotatedConstructor) =>
            CaseClassCreator(
              jsonCreatorAnnotatedConstructor.getAnnotated,
              getBeanPropertyDefinitions(
                jsonCreatorAnnotatedConstructor.getAnnotated.getParameters,
                jsonCreatorAnnotatedConstructor)
            )
          case _ =>
            // try to use what Jackson thinks is the default -- however Jackson does not
            // seem to correctly track an empty default constructor for case classes, nor
            // multiple un-annotated and we have no way to pick a proper constructor so we bail
            val constructor = Option(beanDescription.getClassInfo.getDefaultConstructor) match {
              case Some(ctor) => ctor
              case _ =>
                val constructors = beanDescription.getBeanClass.getConstructors
                assert(constructors.size == 1, "Multiple case class constructors not supported")
                annotateConstructor(constructors.head, clazzAnnotations)
            }
            CaseClassCreator(
              constructor.getAnnotated,
              getBeanPropertyDefinitions(constructor.getAnnotated.getParameters, constructor)
            )
        }
    }
  }
  // nested class inside another class is not supported, e.g., we do not support
  // use of creators for non-static inner classes,
  assert(!beanDescription.isNonStaticInnerClass, "Non-static inner case classes are not supported.")

  // all class annotations -- including inherited annotations for fields
  private[this] val allAnnotations: scala.collection.Map[String, Array[Annotation]] = {
    // use the carried scalaType in order to find the appropriate constructor by the
    // unresolved scalaTypes -- e.g., before we resolve generic types to their bound type
    AnnotationUtils.findAnnotations(
      clazz,
      caseClazzCreator.propertyDefinitions
        .map(_.scalaType.erasure),
      caseClazzCreator.propertyDefinitions
        .map(_.beanPropertyDefinition.getInternalName)
    )
  }

  // support for reading annotations from Jackson Mix-ins
  // see: https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations
  private[this] val allMixinAnnotations: scala.collection.Map[String, Array[Annotation]] =
    mixinClazz match {
      case Some(clazz) =>
        clazz.getDeclaredMethods.map(m => m.getName -> m.getAnnotations).toMap
      case _ =>
        scala.collection.Map.empty[String, Array[Annotation]]
    }

  // optimized lookup of bean property definition annotations
  private[this] def getBeanPropertyDefinitionAnnotations(
    beanPropertyDefinition: BeanPropertyDefinition
  ): Array[Annotation] = {
    if (beanPropertyDefinition.getPrimaryMember.getAllAnnotations.size() > 0) {
      beanPropertyDefinition.getPrimaryMember.getAllAnnotations.annotations().asScala.toArray
    } else Array.empty[Annotation]
  }

  // Field name to list of parsed annotations. Jackson only tracks JacksonAnnotations
  // in the BeanPropertyDefinition AnnotatedMembers and we want to track all class annotations by field.
  // Annotations are keyed by parameter name because the logic collapses annotations from the
  // inheritance hierarchy where the discriminator is member name.
  // note: Prefer while loop over Scala for loop for better performance. The scala for loop
  // performance is optimized in 2.13.0 if we enable scalac: https://github.com/scala/bug/issues/1338
  private[this] val fieldAnnotations: scala.collection.Map[String, Array[Annotation]] = {
    val fieldBeanPropertyDefinitions: Array[BeanPropertyDefinition] =
      caseClazzCreator.propertyDefinitions.map(_.beanPropertyDefinition)
    val collectedFieldAnnotations = mutable.HashMap[String, Array[Annotation]]()
    var index = 0
    while (index < fieldBeanPropertyDefinitions.length) {
      val fieldBeanPropertyDefinition = fieldBeanPropertyDefinitions(index)
      val fieldName = fieldBeanPropertyDefinition.getInternalName
      // in many cases we will have the field annotations in the `allAnnotations` Map which
      // scans for annotations from the class definition being deserialized, however in some cases
      // we are dealing with an annotated field from a static or secondary constructor
      // and thus the annotations may not exist in the `allAnnotations` Map so we default to
      // any carried bean property definition annotations which includes annotation information
      // for any static or secondary constructor.
      val annotations =
        allAnnotations.getOrElse(
          fieldName,
          getBeanPropertyDefinitionAnnotations(fieldBeanPropertyDefinition)
        ) ++ allMixinAnnotations.getOrElse(fieldName, Array.empty)
      if (annotations.nonEmpty) collectedFieldAnnotations.put(fieldName, annotations)
      index += 1
    }

    collectedFieldAnnotations
  }

  private[this] val annotatedValidationClazz: Option[ValidationDescriptor] =
    validator match {
      case Some(v) =>
        Some(createConstructionValidationClass(v, clazz, fieldAnnotations))
      case _ =>
        None
    }

  private[this] def createConstructionValidationClass(
    validator: Validator,
    clazz: Class[_],
    annotationsMap: Map[String, Array[Annotation]]
  ): ValidationDescriptor = {
    val annotatedFieldsMap = for {
      (name, annotations) <- annotationsMap
    } yield {
      val fieldValidators = for {
        annotation <- annotations if Validator.isConstraintAnnotation(annotation)
      } yield validator.findFieldValidator(annotation)

      (
        name,
        ValidationAnnotatedField(
          Some(name),
          Path(name),
          // the annotation may be from a static or secondary constructor
          // for which we have no field information
          Try(clazz.getDeclaredField(name)).toOption,
          fieldValidators
        )
      )
    }
    ValidationDescriptor(
      clazz,
      Path.Empty,
      annotatedFieldsMap,
      Validator.getMethodValidations(clazz))
  }

  /* exposed for testing */
  private[jackson] val fields: Array[CaseClassField] =
    CaseClassField.createFields(
      clazz,
      caseClazzCreator.executable,
      clazzDescriptor,
      caseClazzCreator.propertyDefinitions,
      fieldAnnotations,
      propertyNamingStrategy,
      config.getTypeFactory,
      injectableTypes
    )

  private[this] lazy val numConstructorArgs = fields.length
  private[this] lazy val isWrapperClass = classOf[WrappedValue[_]].isAssignableFrom(clazz)
  private[this] lazy val firstFieldName = fields.head.name

  /* Public */

  override def isCachable: Boolean = true

  override def deserialize(jsonParser: JsonParser, context: DeserializationContext): Object = {
    if (isWrapperClass)
      deserializeWrapperClass(jsonParser, context)
    else
      deserializeNonWrapperClass(jsonParser, context)
  }

  /* Private */

  private[this] def deserializeWrapperClass(
    jsonParser: JsonParser,
    context: DeserializationContext
  ): Object = {
    if (jsonParser.getCurrentToken.isStructStart) {
      try {
        context.handleUnexpectedToken(
          clazz,
          jsonParser.currentToken(),
          jsonParser,
          "Unable to deserialize wrapped value from a json object"
        )
      } catch {
        case NonFatal(e) =>
          // wrap in a JsonMappingException
          throw JsonMappingException.from(jsonParser, e.getMessage)
      }
    }

    val jsonNode = context.getNodeFactory.objectNode()
    jsonNode.put(firstFieldName, jsonParser.getText)
    deserialize(jsonParser, context, jsonNode)
  }

  private[this] def deserializeNonWrapperClass(
    jsonParser: JsonParser,
    context: DeserializationContext
  ): Object = {
    incrementParserToFirstField(jsonParser, context)
    val jsonNode = jsonParser.readValueAsTree[JsonNode]
    deserialize(jsonParser, context, jsonNode)
  }

  private[this] def deserialize(
    jsonParser: JsonParser,
    context: DeserializationContext,
    jsonNode: JsonNode
  ): Object = {
    val jsonFieldNames: Seq[String] = jsonNode.fieldNames().asScala.toSeq
    val caseClassFieldNames: Seq[String] = fields.map(_.name)
    val unknownFields: Seq[String] = unknownProperties(context, jsonFieldNames, caseClassFieldNames)

    val (values, errors) = if (unknownFields.nonEmpty) {
      // more incoming fields in the JSON than are defined in the case class
      (
        Seq.empty[Object].toArray,
        unknownFieldErrors(
          jsonParser,
          caseClassFieldNames,
          jsonFieldNames.diff(caseClassFieldNames)))
    } else {
      parseConstructorValues(jsonParser, context, jsonNode)
    }

    createAndValidate(values, errors)
  }

  /** Return all "unknown" properties sent in the incoming JSON */
  private[this] def unknownProperties(
    context: DeserializationContext,
    jsonFieldNames: Seq[String],
    caseClassFieldNames: Seq[String]
  ): Seq[String] = {
    // if there is a JsonIgnoreProperties annotation on the class, it should prevail
    val nonIgnoredFields: Seq[String] =
      AnnotationUtils.findAnnotation[JsonIgnoreProperties](clazzAnnotations) match {
        case Some(annotation) if !annotation.ignoreUnknown() =>
          // has a JsonIgnoreProperties annotation and is configured to NOT ignore unknown properties
          val annotationIgnoredFields: Seq[String] = annotation.value()
          // only non-ignored json fields should be considered
          jsonFieldNames.diff(annotationIgnoredFields)
        case None if context.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) =>
          // no annotation but feature is configured, thus all json fields should be considered
          jsonFieldNames
        case _ =>
          Seq.empty[String] // every field is ignorable
      }

    // if we have any non ignored field, return the difference
    if (nonIgnoredFields.nonEmpty) {
      nonIgnoredFields.diff(caseClassFieldNames)
    } else Seq.empty[String]
  }

  /** Return the list of [[CaseClassFieldMappingException]] per unknown field */
  private[this] def unknownFieldErrors(
    jsonParser: JsonParser,
    caseClassFieldNames: Seq[String],
    unknownFields: Seq[String]
  ): Seq[CaseClassFieldMappingException] = {
    unknownFields.map { field =>
      CaseClassFieldMappingException(
        CaseClassFieldMappingException.PropertyPath.Empty,
        ValidationResult.Invalid(
          message = UnrecognizedPropertyException
            .from(
              jsonParser,
              clazz,
              field,
              caseClassFieldNames.map(_.asInstanceOf[Object]).asJavaCollection
            ).getMessage
        )
      )
    }
  }

  private[this] def incrementParserToFirstField(
    jsonParser: JsonParser,
    context: DeserializationContext
  ): Unit = {
    if (jsonParser.getCurrentToken == JsonToken.START_OBJECT) {
      jsonParser.nextToken()
    }
    if (jsonParser.getCurrentToken != JsonToken.FIELD_NAME &&
      jsonParser.getCurrentToken != JsonToken.END_OBJECT) {
      try {
        context.handleUnexpectedToken(clazz, jsonParser)
      } catch {
        case NonFatal(e) =>
          e match {
            case j: JsonProcessingException =>
              // don't include source info since it's often blank.
              j.clearLocation()
              throw new JsonParseException(jsonParser, j.getMessage)
            case _ =>
              throw new JsonParseException(jsonParser, e.getMessage)
          }
      }
    }
  }

  // note: Prefer while loop over Scala for loop for better performance. The scala for loop
  // performance is optimized in 2.13.0 if we enable scalac: https://github.com/scala/bug/issues/1338
  private[this] def parseConstructorValues(
    jsonParser: JsonParser,
    context: DeserializationContext,
    jsonNode: JsonNode
  ): (Array[Object], mutable.ArrayBuffer[CaseClassFieldMappingException]) = {
    /* Mutable Fields */
    var constructorValuesIdx = 0
    val constructorValues = new Array[Object](numConstructorArgs)
    val errors = mutable.ArrayBuffer[CaseClassFieldMappingException]()

    while (constructorValuesIdx < numConstructorArgs) {
      val field = fields(constructorValuesIdx)
      try {
        val value = field.parse(context, jsonParser.getCodec, jsonNode)
        constructorValues(constructorValuesIdx) = value //mutation

        annotatedValidationClazz match {
          case Some(annotatedClazz) if annotatedClazz.fields.nonEmpty =>
            val fieldValidationErrors =
              executeFieldValidations(value, field, annotatedClazz.fields)
            append(errors, fieldValidationErrors)
          case _ => // do nothing
        }
      } catch {
        case e: CaseClassFieldMappingException =>
          if (e.path == null) {
            // fill in missing path details
            addException(
              field,
              e.withPropertyPath(CaseClassFieldMappingException.PropertyPath.leaf(field.name)),
              constructorValues,
              constructorValuesIdx,
              errors
            )
          } else {
            addException(
              field,
              e,
              constructorValues,
              constructorValuesIdx,
              errors
            )
          }
        case e: org.joda.time.IllegalFieldValueException =>
          // don't catch just IllegalArgumentException as that hides errors from validating an incorrect type
          val ex = CaseClassFieldMappingException(
            CaseClassFieldMappingException.PropertyPath.leaf(field.name),
            Invalid(e.getMessage, ValidationErrorCode.Unknown)
          )
          addException(
            field,
            ex,
            constructorValues,
            constructorValuesIdx,
            errors
          )
        case e: InvalidFormatException =>
          addException(
            field,
            CaseClassFieldMappingException(
              CaseClassFieldMappingException.PropertyPath.leaf(field.name),
              Invalid(
                s"'${e.getValue.toString}' is not a " +
                  s"valid ${Types.wrapperType(e.getTargetType).getSimpleName}${validValuesString(e)}",
                ErrorCode.JsonProcessingError(e)
              )
            ),
            constructorValues,
            constructorValuesIdx,
            errors
          )
        case e: MismatchedInputException =>
          addException(
            field,
            CaseClassFieldMappingException(
              CaseClassFieldMappingException.PropertyPath.leaf(field.name),
              Invalid(
                s"'${jsonNode.asText("")}' is not a " +
                  s"valid ${Types.wrapperType(e.getTargetType).getSimpleName}${validValuesString(e)}",
                ErrorCode.JsonProcessingError(e)
              )
            ),
            constructorValues,
            constructorValuesIdx,
            errors
          )
        case e: CaseClassMappingException =>
          constructorValues(constructorValuesIdx) = field.missingValue //mutation
          errors ++= e.errors.map(_.scoped(field.name))
        case e: JsonProcessingException =>
          // don't include source info since it's often blank. Consider adding e.getCause.getMessage
          e.clearLocation()
          addException(
            field,
            CaseClassFieldMappingException(
              CaseClassFieldMappingException.PropertyPath.leaf(field.name),
              Invalid(e.errorMessage, ErrorCode.JsonProcessingError(e))
            ),
            constructorValues,
            constructorValuesIdx,
            errors
          )
        case e: java.util.NoSuchElementException
            if isScalaEnumerationType(field.javaType.getRawClass) =>
          // Scala enumeration mapping issue
          addException(
            field,
            CaseClassFieldMappingException(
              CaseClassFieldMappingException.PropertyPath.leaf(field.name),
              Invalid(
                e.getMessage,
                ValidationErrorCode.Unknown
              )
            ),
            constructorValues,
            constructorValuesIdx,
            errors
          )
        case e: InjectableValuesException =>
          // we rethrow, to prevent leaking internal injection details in the "errors" array
          throw e
        case NonFatal(e) =>
          error("Unexpected exception parsing field: " + field, e)
          throw e
      }
      constructorValuesIdx += 1
    }

    (constructorValues, errors)
  }

  private[this] def isScalaEnumerationType(clazz: Class[_]): Boolean =
    clazz.getName.startsWith(classOf[scala.Enumeration].getName)

  /** Add the given exception to the given array buffer of errors while also adding a missing value field to the given array */
  private[this] def addException(
    field: CaseClassField,
    e: CaseClassFieldMappingException,
    array: Array[Object],
    idx: Int,
    errors: mutable.ArrayBuffer[CaseClassFieldMappingException]
  ): Unit = {
    array(idx) = field.missingValue //mutation
    errors += e //mutation
  }

  private[this] def validValuesString(e: MismatchedInputException): String = {
    if (e.getTargetType != null && e.getTargetType.isEnum)
      " with valid values: " + e.getTargetType.getEnumConstants.mkString(", ")
    else
      ""
  }

  private[this] def createAndValidate(
    constructorValues: Array[Object],
    fieldErrors: Seq[CaseClassFieldMappingException]
  ): Object = {
    if (fieldErrors.nonEmpty) {
      throw CaseClassMappingException(fieldErrors.toSet)
    }

    val obj = create(constructorValues)
    annotatedValidationClazz match {
      case Some(annotatedClazz) if annotatedClazz.methods.nonEmpty =>
        executeMethodValidations(fieldErrors, obj, annotatedClazz.methods)
      case _ => // do nothing
    }
    obj
  }

  private[this] def create(constructorValues: Array[Object]): Object = {
    try {
      caseClazzCreator.executable match {
        case method: Method =>
          // if the creator is of type Method, we assume the need to invoke the companion object
          method.invoke(clazzDescriptor.companion.get.instance, constructorValues: _*)
        case const: Constructor[_] =>
          // otherwise simply invoke the constructor
          const.newInstance(constructorValues: _*).asInstanceOf[Object]
      }
    } catch {
      case e @ (_: InvocationTargetException | _: ExceptionInInitializerError) =>
        // propagate the underlying cause of the failed instantiation if available
        // TODO: use DeserializationContext.handleInstantiationProblem which will wrap the cause in a JsonMappingException
        if (e.getCause == null) throw e else throw e.getCause
    }
  }

  // Validate `Constraint` annotations for each field.
  //
  // fields: All field's `Constraint` annotations and their matching `ConstraintValidator`s are stored in
  // `ValidationAnnotationField`, keyed by the field name.
  private[this] def executeFieldValidations(
    value: Any,
    field: CaseClassField,
    fields: Map[String, ValidationAnnotatedField]
  ): Seq[CaseClassFieldMappingException] = {
    validator match {
      case Some(v) =>
        for {
          invalid @ Invalid(_, _, _, _) <- v.validateField(
            value,
            field.beanPropertyDefinition.getInternalName,
            fieldValidators = fields.get(field.beanPropertyDefinition.getInternalName) match {
              case Some(annotatedField) => annotatedField.fieldValidators
              case _ => Array.empty[FieldValidator]
            }
          )
        } yield {
          CaseClassFieldMappingException(
            CaseClassFieldMappingException.PropertyPath.leaf(field.name),
            invalid)
        }
      case _ =>
        Seq.empty[CaseClassFieldMappingException]
    }
  }

  // Validate all methods annotated with `MethodValidation` defined in the deserialized case class.
  // This is called after the case class is created.
  //
  // obj: the deserialized case class
  // methods: all methods and their annotations are stored in `ValidationAnnotatedMethod`, and is
  //          passed in to this method as the value of `methods` parameter.
  // fieldErrors: all failed field validations from
  private[this] def executeMethodValidations(
    fieldErrors: Seq[CaseClassFieldMappingException],
    obj: Any,
    methods: Array[ValidationAnnotatedMethod]
  ): Unit = {
    if (validator.isDefined) {
      // only run method validations if we have a configured Validator
      val methodValidationErrors =
        new mutable.ArrayBuffer[Iterable[CaseClassFieldMappingException]]()
      Validator.validateMethods(obj, methods) {
        case (_, invalid) =>
          val caseClassFields = Validator.extractFieldsFromMethodValidation(invalid.annotation)
          val exceptions = caseClassFields
            .map(
              CaseClassFieldMappingException.PropertyPath.leaf
            ) // per field, create a new PropertyPath with the given field as the leaf
            .map(
              CaseClassFieldMappingException(_, invalid)
            ) // create a new CaseClassFieldMappingException
          // with the given PropertyPath and the ValidationResult.Invalid
          if (exceptions.isEmpty) {
            methodValidationErrors.append(
              Seq(
                CaseClassFieldMappingException(
                  CaseClassFieldMappingException.PropertyPath.Empty,
                  invalid)))
          } else {
            methodValidationErrors.append(exceptions)
          }
      }

      if (methodValidationErrors.nonEmpty) {
        throw CaseClassMappingException(fieldErrors.toSet ++ methodValidationErrors.flatten)
      }
    }
  }

  private[this] def propertyNamingStrategy: PropertyNamingStrategy = {
    if (AnnotationUtils.findAnnotation[JsonCamelCase](clazzAnnotations).isDefined) {
      PropertyNamingStrategy.LOWER_CAMEL_CASE
    } else {
      AnnotationUtils.findAnnotation[JsonNaming](clazzAnnotations) match {
        case Some(jsonNaming)
            if !jsonNaming.value().isAssignableFrom(classOf[PropertyNamingStrategy]) =>
          jsonNaming.value().newInstance()
        case _ =>
          config.getPropertyNamingStrategy
      }
    }
  }

  // optimized
  private[this] def append[T](buffer: mutable.ArrayBuffer[T], seqToAppend: Seq[T]): Unit =
    if (seqToAppend.nonEmpty) buffer ++= seqToAppend

  private[this] def annotateConstructor(
    constructor: Constructor[_],
    annotations: Seq[Annotation]
  ): AnnotatedConstructor = {
    val paramAnnotationMaps: Array[AnnotationMap] = {
      constructor.getParameterAnnotations.map { parameterAnnotations =>
        val parameterAnnotationMap = new AnnotationMap()
        parameterAnnotations.map(parameterAnnotationMap.add)
        parameterAnnotationMap
      }
    }

    val annotationMap = new AnnotationMap()
    annotations.map(annotationMap.add)
    new AnnotatedConstructor(
      new TypeResolutionContext.Basic(config.getTypeFactory, javaType.getBindings),
      constructor,
      annotationMap,
      paramAnnotationMaps
    )
  }

  /* in order to deal with parameterized types we create a JavaType here and carry it */
  private[this] def getBeanPropertyDefinitions(
    parameters: Array[Parameter],
    annotatedWithParams: AnnotatedWithParams,
    fromCompanion: Boolean = false
  ): Array[PropertyDefinition] = {
    // need to find the scala description which carries the full type information
    val constructorParamDescriptors =
      findConstructorDescriptor(parameters) match {
        case Some(constructorDescriptor) =>
          constructorDescriptor.params
        case _ =>
          throw InvalidDefinitionException.from(
            CaseClassDeserializer.EmptyJsonParser,
            s"Unable to locate suitable constructor for class: ${clazz.getName}",
            javaType)
      }

    for ((parameter, index) <- parameters.zipWithIndex) yield {
      val constructorParamDescriptor = constructorParamDescriptors(index)
      val scalaType = constructorParamDescriptor.argType

      val parameterJavaType =
        if (!javaType.getBindings.isEmpty &&
          shouldFullyDefineParameterizedType(scalaType, parameter)) {
          // what types are bound to the generic case class parameters
          val boundTypeParameters: Array[JavaType] =
            TypeUtils
              .parameterizedTypeNames(parameter.getParameterizedType)
              .map(javaType.getBindings.findBoundType)
          Types
            .javaType(
              config.getTypeFactory,
              scalaType,
              boundTypeParameters
            )
        } else {
          Types.javaType(config.getTypeFactory, scalaType)
        }

      val annotatedParameter =
        newAnnotatedParameter(
          typeResolutionContext = new TypeResolutionContext.Basic(
            config.getTypeFactory,
            javaType.getBindings
          ), // use the TypeBindings from the top-level JavaType, not the parameter JavaType
          owner = annotatedWithParams,
          annotations = annotatedWithParams.getParameterAnnotations(index),
          javaType = parameterJavaType,
          index = index
        )

      PropertyDefinition(
        parameterJavaType,
        scalaType,
        SimpleBeanPropertyDefinition
          .construct(
            config,
            annotatedParameter,
            new PropertyName(constructorParamDescriptor.name)
          )
      )
    }
  }

  // note: Prefer while loop over Scala for loop for better performance. The scala for loop
  // performance is optimized in 2.13.0 if we enable scalac: https://github.com/scala/bug/issues/1338
  private[this] def findConstructorDescriptor(
    parameters: Array[Parameter]
  ): Option[ConstructorDescriptor] = {
    val constructors = clazzDescriptor.constructors
    var index = 0
    while (index < constructors.length) {
      val constructorDescriptor = constructors(index)
      val params = constructorDescriptor.params
      if (params.length == parameters.length) {
        // description has the same number of parameters we're looking for, check each type in order
        val checkedParams = params.map { param =>
          Types
            .wrapperType(param.argType.erasure)
            .isAssignableFrom(Types.wrapperType(parameters(param.argIndex).getType))
        }
        if (checkedParams.forall(_ == true)) return Some(constructorDescriptor)
      }
      index += 1
    }
    None
  }

  // if we need to attempt to fully specify the JavaType because it is generically types
  private[this] def shouldFullyDefineParameterizedType(
    scalaType: ScalaType,
    parameter: Parameter
  ): Boolean = {
    // only need to fully specify if the type is parameterized and it has more than one type arg
    // or its typeArg is also parameterized.
    def isParameterized(reflectionType: Type): Boolean = reflectionType match {
      case _: ParameterizedType | _: TypeVariable[_] =>
        true
      case _ =>
        false
    }

    val parameterizedType = parameter.getParameterizedType
    !scalaType.isPrimitive &&
    (scalaType.typeArgs.isEmpty ||
    scalaType.typeArgs.head.erasure.isAssignableFrom(classOf[Object])) &&
    parameterizedType != parameter.getType &&
    isParameterized(parameterizedType)
  }
}
