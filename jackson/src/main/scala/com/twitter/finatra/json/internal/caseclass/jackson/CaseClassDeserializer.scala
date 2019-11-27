package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.core.{
  JsonParseException,
  JsonParser,
  JsonProcessingException,
  JsonToken
}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.exc.{InvalidFormatException, MismatchedInputException}
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassValidationException.PropertyPath
import com.twitter.finatra.json.internal.caseclass.exceptions._
import com.twitter.finatra.response.JsonCamelCase
import com.twitter.finatra.validation.ValidationResult.Invalid
import com.twitter.finatra.validation.{ErrorCode, MethodValidation, ValidationProvider}
import com.twitter.inject.Logging
import com.twitter.inject.domain.WrappedValue
import java.lang.annotation.Annotation
import java.lang.reflect.InvocationTargetException
import javax.annotation.concurrent.ThreadSafe
import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

/**
 * Custom case class deserializer which overcomes limitations in jackson-scala-module.
 *
 * Our improvements:
 * - Throw a JsonException when 'non Option' fields are missing in the incoming json
 * - Use default values when fields are missing in the incoming json
 * - Properly deserialize a Seq[Long] (see https://github.com/FasterXML/jackson-module-scala/issues/62)
 * - Support "wrapped values" using WrappedValue (needed since jackson-scala-module does not support @JsonCreator)
 * - Support for field and method level validations
 *
 * @note This class is inspired by Jerkson's CaseClassDeserializer which can be found here:
 * [[https://github.com/codahale/jerkson/blob/master/src/main/scala/com/codahale/jerkson/deser/CaseClassDeserializer.scala]]
 */
@ThreadSafe
private[finatra] class CaseClassDeserializer(
  javaType: JavaType,
  config: DeserializationConfig,
  beanDesc: BeanDescription,
  validationProvider: ValidationProvider)
    extends JsonDeserializer[AnyRef]
    with Logging {

  private val caseClassFields =
    CaseClassField.createFields(
      javaType.getRawClass,
      propertyNamingStrategy,
      config.getTypeFactory,
      validationProvider
    )
  private val typeBindings: Map[String, JavaType] = parseTypeBindings

  private val numConstructorArgs = caseClassFields.size
  private val constructor = {
    val constructors = javaType.getRawClass.getConstructors
    assert(constructors.size == 1, "Multiple case class constructors not supported")
    constructors.head
  }
  private val isWrapperClass = classOf[WrappedValue[_]].isAssignableFrom(javaType.getRawClass)
  private val validationManager = validationProvider()
  private lazy val firstFieldName = caseClassFields.head.name

  /* Public */

  override def isCachable: Boolean = true

  override def deserialize(jp: JsonParser, context: DeserializationContext): Object = {
    if (isWrapperClass)
      deserializeWrapperClass(jp, context)
    else
      deserializeNonWrapperClass(jp, context)
  }

  /* Private */

  private def deserializeWrapperClass(jp: JsonParser, context: DeserializationContext): Object = {
    if (jp.getCurrentToken.isStructStart) {
      try {
        context.handleUnexpectedToken(
          javaType.getRawClass,
          jp.currentToken(),
          jp,
          "Unable to deserialize wrapped value from a json object"
        )
      } catch {
        case NonFatal(e) =>
          throw JsonMappingException.from(jp, e.getMessage)
      }
    }

    val jsonNode = context.getNodeFactory.objectNode()
    jsonNode.put(firstFieldName, jp.getText)
    deserialize(jp, context, jsonNode)
  }

  private def deserializeNonWrapperClass(
    jp: JsonParser,
    context: DeserializationContext
  ): Object = {
    incrementParserToFirstField(jp, context)
    val jsonNode = jp.readValueAsTree[JsonNode]
    deserialize(jp, context, jsonNode)
  }

  private def deserialize(
    jp: JsonParser,
    context: DeserializationContext,
    jsonNode: JsonNode
  ): Object = {
    val (values, errors) = parseConstructorValues(jp, context, jsonNode)
    createAndValidate(values, errors)
  }

  private def incrementParserToFirstField(jp: JsonParser, ctxt: DeserializationContext): Unit = {
    if (jp.getCurrentToken == JsonToken.START_OBJECT) {
      jp.nextToken()
    }
    if (jp.getCurrentToken != JsonToken.FIELD_NAME &&
      jp.getCurrentToken != JsonToken.END_OBJECT) {
      try {
        ctxt.handleUnexpectedToken(javaType.getRawClass, jp)
      } catch {
        case NonFatal(e) =>
          throw new JsonParseException(jp, e.getMessage)
      }
    }
  }

  // optimized
  private def parseConstructorValues(
    jp: JsonParser,
    context: DeserializationContext,
    jsonNode: JsonNode
  ): (Array[Object], ArrayBuffer[CaseClassValidationException]) = {
    /* Mutable Fields */
    var constructorValuesIdx = 0
    val constructorValues = new Array[Object](numConstructorArgs)
    val errors = ArrayBuffer[CaseClassValidationException]()

    def addConstructorValue(value: Object): Unit = {
      constructorValues(constructorValuesIdx) = value //mutation
      constructorValuesIdx += 1 //mutation
    }
    def addException(field: CaseClassField, e: CaseClassValidationException): Unit = {
      addConstructorValue(field.missingValue)
      errors += e //mutation
    }

    for (field <- caseClassFields) {
      try {
        val value = field.parse(context, jp.getCodec, jsonNode, typeBindings)
        addConstructorValue(value)

        if (field.validationAnnotations.nonEmpty) {
          val fieldValidationErrors = executeFieldValidations(value, field)
          append(errors, fieldValidationErrors)
        }
      } catch {
        case e: CaseClassValidationException =>
          addException(field, e)
        case e: org.joda.time.IllegalFieldValueException =>
          // don't catch just IllegalArgumentException as that hides errors from validating an incorrect type
          val ex = CaseClassValidationException(
            PropertyPath.leaf(field.name),
            Invalid(e.getMessage, ErrorCode.Unknown)
          )
          addException(field, ex)
        case e: InvalidFormatException =>
          addException(
            field,
            CaseClassValidationException(
              PropertyPath.leaf(field.name),
              Invalid(
                s"'${e.getValue.toString}' is not a " +
                  s"valid ${boxedClassName(e.getTargetType.getSimpleName)}${validValuesString(e)}",
                ErrorCode.JsonProcessingError(e)
              )
            )
          )
        case e: MismatchedInputException =>
          addException(
            field,
            CaseClassValidationException(
              PropertyPath.leaf(field.name),
              Invalid(
                s"'${jsonNode.asText("")}' is not a " +
                  s"valid ${boxedClassName(e.getTargetType.getSimpleName)}${validValuesString(e)}",
                ErrorCode.JsonProcessingError(e)
              )
            )
          )
        case e: CaseClassMappingException =>
          addConstructorValue(field.missingValue)
          errors ++= e.errors map { _.scoped(field) }
        case e: JsonProcessingException =>
          // don't include source info since it's often blank. Consider adding e.getCause.getMessage
          addException(
            field,
            CaseClassValidationException(
              PropertyPath.leaf(field.name),
              Invalid(JacksonUtils.errorMessage(e), ErrorCode.JsonProcessingError(e))
            )
          )
        case _: RepeatedCommaSeparatedQueryParameterException =>
          addException(
            field,
            CaseClassValidationException(
              PropertyPath.leaf(field.name),
              Invalid(
                s"Repeating ${field.name} is not allowed. Pass multiple values as a single comma-separated string.",
                ErrorCode.RepeatedCommaSeparatedCollection)
            )
          )
        case e @ (_: JsonInjectionNotSupportedException | _: JsonInjectException) =>
          // we rethrow, to prevent leaking internal injection details in the "errors" array
          throw e
        case NonFatal(e) =>
          error("Unexpected exception parsing field: " + field, e)
          throw e
      }
    }

    (constructorValues, errors)
  }

  private def validValuesString(e: MismatchedInputException): String = {
    if (e.getTargetType.isEnum)
      " with valid values: " + e.getTargetType.getEnumConstants.mkString(", ")
    else
      ""
  }

  private def createAndValidate(
    constructorValues: Array[Object],
    fieldErrors: Seq[CaseClassValidationException]
  ): Object = {
    if (fieldErrors.nonEmpty) {
      throw CaseClassMappingException(fieldErrors.toSet)
    }

    val obj = create(constructorValues)
    executeMethodValidations(fieldErrors, obj)
    obj
  }

  private def create(constructorValues: Array[Object]): Object = {
    try {
      constructor.newInstance(constructorValues: _*).asInstanceOf[Object]
    } catch {
      case e @ (_: InvocationTargetException | _: ExceptionInInitializerError) =>
        // propagate the underlying cause of the failed instantiation if available
        if (e.getCause == null) throw e else throw e.getCause
    }
  }

  private def executeFieldValidations(
    value: Any,
    field: CaseClassField
  ): Seq[CaseClassValidationException] = {
    for {
      invalid @ Invalid(_, _, _) <- validationManager.validateField(
        value,
        field.validationAnnotations)
    } yield {
      CaseClassValidationException(PropertyPath.leaf(field.name), invalid)
    }
  }

  private def executeMethodValidations(
    fieldErrors: Seq[CaseClassValidationException],
    obj: Any
  ): Unit = {
    val methodValidationErrors: Seq[Seq[CaseClassValidationException]] = for {
      invalid @ Invalid(_, _, _) <- validationManager.validateMethods(obj)
      fields = extractFieldsFromAnnotation(invalid.annotation)
      propertyPaths = fields.map(PropertyPath.leaf)
      exceptions = propertyPaths.map(CaseClassValidationException(_, invalid))
    } yield {
      if (exceptions.isEmpty) {
        Seq(CaseClassValidationException(PropertyPath.empty, invalid))
      } else {
        exceptions
      }
    }

    if (methodValidationErrors.nonEmpty) {
      throw CaseClassMappingException(fieldErrors.toSet ++ methodValidationErrors.flatten.toSet)
    }
  }

  private def propertyNamingStrategy = {
    if (javaType.getRawClass.isAnnotationPresent(classOf[JsonCamelCase]))
      PropertyNamingStrategy.LOWER_CAMEL_CASE
    else
      config.getPropertyNamingStrategy
  }

  //optimized
  private[this] def append[T](buffer: ArrayBuffer[T], seqToAppend: Seq[T]): Unit = {
    if (seqToAppend.nonEmpty) {
      buffer ++= seqToAppend
    }
  }

  private[this] def extractFieldsFromAnnotation(annotation: Option[Annotation]): Seq[String] = {
    annotation match {
      case Some(annot) if annot.isInstanceOf[MethodValidation] =>
        annot.asInstanceOf[MethodValidation].fields().filter(_.nonEmpty)
      case _ =>
        Seq.empty[String]
    }
  }

  private[this] def boxedClassName(name: String): String = name match {
    case "boolean" => classOf[java.lang.Boolean].getSimpleName
    case "byte" => classOf[java.lang.Byte].getSimpleName
    case "short" => classOf[java.lang.Short].getSimpleName
    case "char" => classOf[java.lang.Character].getSimpleName
    case "int" => classOf[java.lang.Integer].getSimpleName
    case "long" => classOf[java.lang.Long].getSimpleName
    case "float" => classOf[java.lang.Float].getSimpleName
    case "double" => classOf[java.lang.Double].getSimpleName
    case _ => name
  }

  private[this] def parseTypeBindings: Map[String, JavaType] =
    Option(javaType.getBindings) match {
      case Some(bindings) =>
        val m = scala.collection.mutable.Map[String, JavaType]()
        for (i <- 0 to bindings.size()) {
          val key = bindings.getBoundName(i) // we assume value is not null if key is not null
          if (key != null) m += key -> bindings.getBoundType(i)
        }
        m.toMap
      case _ => Map.empty[String, JavaType]
    }
}
