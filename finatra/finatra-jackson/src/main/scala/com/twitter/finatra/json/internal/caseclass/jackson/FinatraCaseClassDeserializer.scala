package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.core.{JsonParser, JsonProcessingException, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.twitter.finatra.conversions.booleans._
import com.twitter.finatra.json.annotations.JsonCamelCase
import com.twitter.finatra.json.internal.caseclass.CaseClassField
import com.twitter.finatra.json.internal.caseclass.exceptions._
import com.twitter.finatra.json.internal.caseclass.utils.NamingStrategyUtils
import com.twitter.finatra.json.internal.caseclass.validation.{ValidationManager, ValidationMessageResolver}
import com.twitter.finatra.json.internal.caseclass.wrapped.JsonWrappedValue
import com.twitter.finatra.utils.Logging
import com.twitter.util.NonFatal
import javax.annotation.concurrent.ThreadSafe
import scala.collection.mutable.ArrayBuffer

/**
 * Custom case class deserializer to overcome limitations in jackson-scala-module
 *
 * The limitations which are fixed below are:
 * * We throw a JsonException when 'required' fields are missing in the json
 * * We use default values if a field is missing
 * * We properly deserialize a case class with a Seq[Long] (https://github.com/FasterXML/jackson-module-scala/issues/62)
 * * We support "wrapped values" using JsonWrappedValue which would normally use @JsonCreator which is not supported
 *
 * NOTE: This class is inspired by Jerkson' CaseClassDeserializer which can be found here:
 * https://github.com.cloudphysics.jerkson/blob/master/src/main/scala/com.cloudphysics.jerkson/deser/CaseClassDeserializer.scala
 */
@ThreadSafe
class FinatraCaseClassDeserializer(
  javaType: JavaType,
  config: DeserializationConfig,
  beanDesc: BeanDescription)
  extends JsonDeserializer[AnyRef]
  with Logging {

  private val caseClassFields = CaseClassField.createFields(
    javaType.getRawClass,
    propertyNamingStrategy,
    config.getTypeFactory)

  private val numConstructorArgs = caseClassFields.size
  private val constructor = {
    val constructors = javaType.getRawClass.getConstructors
    assert(constructors.size == 1, "Multiple case class constructors not supported")
    constructors.head
  }
  private val isWrapperClass = classOf[JsonWrappedValue[_]].isAssignableFrom(javaType.getRawClass)
  private val messageResolver = new ValidationMessageResolver
  private val validationManager = new ValidationManager(messageResolver)
  private lazy val firstFieldName = caseClassFields.head.name

  /* Public */

  override def isCachable = true

  override def deserialize(jp: JsonParser, context: DeserializationContext): Object = {
    if (isWrapperClass)
      deserializeWrapperClass(jp, context)
    else
      deserializeNonWrapperClass(jp, context)
  }

  /* Private */

  private def deserializeWrapperClass(jp: JsonParser, context: DeserializationContext): Object = {
    val jsonNode = context.getNodeFactory.objectNode()
    jsonNode.put(firstFieldName, jp.getText)
    deserialize(jp, context, jsonNode)
  }

  private def deserializeNonWrapperClass(jp: JsonParser, context: DeserializationContext): Object = {
    incrementParserToFirstField(jp, context)
    val jsonNode = jp.readValueAsTree[JsonNode]
    deserialize(jp, context, jsonNode)
  }

  private def deserialize(jp: JsonParser, context: DeserializationContext, jsonNode: JsonNode): Object = {
    val (values, errors) = parseConstructorValues(jp, context, jsonNode)
    createAndValidate(values, errors)
  }

  private def incrementParserToFirstField(jp: JsonParser, ctxt: DeserializationContext) {
    if (jp.getCurrentToken == JsonToken.START_OBJECT) {
      jp.nextToken()
    }
    if (jp.getCurrentToken != JsonToken.FIELD_NAME && jp.getCurrentToken != JsonToken.END_OBJECT) {
      throw ctxt.mappingException(javaType.getRawClass)
    }
  }

  // optimized
  private def parseConstructorValues(jp: JsonParser, context: DeserializationContext, jsonNode: JsonNode): (Array[Object], ArrayBuffer[JsonFieldParseException]) = {
    /* Mutable Fields */
    var constructorValuesIdx = 0
    val constructorValues = new Array[Object](numConstructorArgs)
    val errors = ArrayBuffer[JsonFieldParseException]()

    def addConstructorValue(value: Object) {
      constructorValues(constructorValuesIdx) = value //mutation
      constructorValuesIdx += 1 //mutation
    }
    def addException(field: CaseClassField, e: JsonFieldParseException) {
      addConstructorValue(field.missingValue)
      errors += e //mutation
    }

    for (field <- caseClassFields) {
      try {
        val value = field.parse(context, jp.getCodec, jsonNode)
        addConstructorValue(value)

        if (field.validationAnnotations.nonEmpty) {
          val fieldValidationErrors = executeFieldValidations(value, field)
          append(errors, fieldValidationErrors)
        }
      } catch {
        case e: JsonFieldParseException =>
          addException(field, e)

        case e: InvalidFormatException =>
          addException(
            field,
            invalidFormatJsonFieldParseError(field, e))

        case e: JsonProcessingException =>
          // Don't include source info since it's often blank. Consider adding e.getCause.getMessage
          addException(
            field,
            jsonProcessingJsonFieldParseError(field, e))

        case e: JsonObjectParseException =>
          addConstructorValue(field.missingValue)
          errors ++= e.fieldErrors map {_.nestFieldName(field)}
          errors ++= e.methodValidationErrors map { ve => methodValidationJsonFieldParseError(field, ve)}

        // we rethrow, to prevent leaking internal injection details in the "errors" array
        case e: JsonInjectionNotSupportedException =>
          throw e
        case e: JsonInjectException =>
          throw e

        case NonFatal(e) =>
          addException(field, JsonFieldParseException("Unexpected exception parsing field: " + field.name))
          error("Unexpected exception parsing field: " + field, e)
      }
    }

    (constructorValues, errors)
  }

  // TODO don't leak class name details in error message
  private def invalidFormatJsonFieldParseError(field: CaseClassField, e: InvalidFormatException): JsonFieldParseException = {
    JsonFieldParseException(
      field.name + "'s value '" + e.getValue + "' is not a valid " + e.getTargetType.getSimpleName + validValuesString(e))
  }

  private def jsonProcessingJsonFieldParseError(field: CaseClassField, e: JsonProcessingException): JsonFieldParseException = {
    JsonFieldParseException(
      field.name + ": " + JacksonUtils.errorMessage(e))
  }

  private def methodValidationJsonFieldParseError(field: CaseClassField, e: JsonMethodValidationException): JsonFieldParseException = {
    JsonFieldParseException(e.msg).nestFieldName(
      field = field,
      methodValidation = true)
  }

  private def validValuesString(e: InvalidFormatException): String = {
    e.getTargetType.isEnum.ifTrue(
      " with valid values: " + e.getTargetType.getEnumConstants.mkString(", "))
  }

  private def createAndValidate(constructorValues: Array[Object], fieldErrors: Seq[JsonFieldParseException]): Object = {
    if (fieldErrors.nonEmpty) {
      throw new JsonObjectParseException(fieldErrors)
    }

    val obj = constructor.newInstance(constructorValues: _*)
    executeMethodValidations(fieldErrors, obj)
    obj.asInstanceOf[Object]
  }

  private def executeFieldValidations(value: Any, field: CaseClassField) = {
    validationManager.validate(value, field.validationAnnotations) map { validationResult =>
      JsonFieldParseException(field.name + " " + validationResult.reason)
    }
  }

  private def executeMethodValidations(fieldErrors: Seq[JsonFieldParseException], obj: Any) {
    val methodValidationErrors = for {
      validationResult <- validationManager.validate(obj)
    } yield JsonMethodValidationException(validationResult.reason)

    if (methodValidationErrors.nonEmpty) {
      throw new JsonObjectParseException(fieldErrors, methodValidationErrors)
    }
  }

  private def propertyNamingStrategy = {
    if (javaType.getRawClass.isAnnotationPresent(classOf[JsonCamelCase]))
      NamingStrategyUtils.CamelCasePropertyNamingStrategy
    else
      config.getPropertyNamingStrategy
  }

  //optimized
  private[this] def append[T](buffer: ArrayBuffer[T], seqToAppend: Seq[T]) {
    if (seqToAppend.nonEmpty) {
      buffer ++= seqToAppend
    }
  }
}
