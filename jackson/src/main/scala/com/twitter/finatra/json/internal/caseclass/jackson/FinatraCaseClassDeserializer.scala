package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.core.{JsonParser, JsonProcessingException, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.twitter.finatra.domain.WrappedValue
import com.twitter.finatra.json.internal.caseclass.exceptions._
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassValidationException.PropertyPath
import com.twitter.finatra.json.internal.caseclass.validation.ValidationManager
import com.twitter.finatra.json.utils.CamelCasePropertyNamingStrategy
import com.twitter.finatra.response.JsonCamelCase
import com.twitter.finatra.validation.{ErrorCode, ValidationMessageResolver}
import com.twitter.finatra.validation.ValidationResult._
import com.twitter.inject.Logging
import com.twitter.util.NonFatal
import java.lang.reflect.InvocationTargetException
import javax.annotation.concurrent.ThreadSafe
import scala.collection.mutable.ArrayBuffer

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
 * NOTE: This class is inspired by Jerkson' CaseClassDeserializer which can be found here:
 * https://github.com/codahale/jerkson/blob/master/src/main/scala/com/codahale/jerkson/deser/CaseClassDeserializer.scala
 */
@ThreadSafe
private[finatra] class FinatraCaseClassDeserializer(
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
  private val isWrapperClass = classOf[WrappedValue[_]].isAssignableFrom(javaType.getRawClass)
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
    if (jp.getCurrentToken.isStructStart) {
      throw context.mappingException("Unable to deserialize wrapped value from a json object")
    }

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
  private def parseConstructorValues(jp: JsonParser, context: DeserializationContext, jsonNode: JsonNode): (Array[Object], ArrayBuffer[CaseClassValidationException]) = {
    /* Mutable Fields */
    var constructorValuesIdx = 0
    val constructorValues = new Array[Object](numConstructorArgs)
    val errors = ArrayBuffer[CaseClassValidationException]()

    def addConstructorValue(value: Object) {
      constructorValues(constructorValuesIdx) = value //mutation
      constructorValuesIdx += 1 //mutation
    }
    def addException(field: CaseClassField, e: CaseClassValidationException) {
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
        case e: CaseClassValidationException =>
          addException(field, e)

        case e: InvalidFormatException =>
          addException(
            field,
            invalidFormatJsonFieldParseError(field, e))

        case e: CaseClassMappingException =>
          addConstructorValue(field.missingValue)
          errors ++= e.errors map {_.scoped(field)}

        case e: JsonProcessingException =>
          // Don't include source info since it's often blank. Consider adding e.getCause.getMessage
          addException(
            field,
            jsonProcessingJsonFieldParseError(field, e))

        // we rethrow, to prevent leaking internal injection details in the "errors" array
        case e: JsonInjectionNotSupportedException =>
          throw e
        case e: JsonInjectException =>
          throw e

        case NonFatal(e) =>
          error("Unexpected exception parsing field: " + field, e)
          throw e
      }
    }

    (constructorValues, errors)
  }

  // TODO don't leak class name details in error message
  private def invalidFormatJsonFieldParseError(field: CaseClassField, e: InvalidFormatException): CaseClassValidationException = {
    CaseClassValidationException(
      PropertyPath.leaf(field.name),
      Invalid(
        s"'${e.getValue}' is not a valid ${e.getTargetType.getSimpleName}${validValuesString(e)}",
        ErrorCode.JsonProcessingError(e)))
  }

  private def jsonProcessingJsonFieldParseError(field: CaseClassField, e: JsonProcessingException): CaseClassValidationException = {
    CaseClassValidationException(
      PropertyPath.leaf(field.name),
      Invalid(
        JacksonUtils.errorMessage(e),
        ErrorCode.JsonProcessingError(e)))
  }

  private def validValuesString(e: InvalidFormatException): String = {
    if (e.getTargetType.isEnum)
      " with valid values: " + e.getTargetType.getEnumConstants.mkString(", ")
    else
      ""
  }

  private def createAndValidate(constructorValues: Array[Object], fieldErrors: Seq[CaseClassValidationException]): Object = {
    if (fieldErrors.nonEmpty) {
      throw new CaseClassMappingException(fieldErrors.toSet)
    }

    val obj = create(constructorValues)
    executeMethodValidations(fieldErrors, obj)
    obj
  }

  private def create(constructorValues: Array[Object]): Object = {
    try {
      constructor.newInstance(constructorValues: _*).asInstanceOf[Object]
    } catch {
      case e@(_: InvocationTargetException | _: ExceptionInInitializerError) =>
        warn("Add validation to avoid instantiating invalid object of type: " + javaType.getRawClass)
        // propagate the underlying cause of the failed instantiation if available
        if (e.getCause == null)
          throw e
        else
          throw e.getCause
    }
  }

  private def executeFieldValidations(value: Any, field: CaseClassField) = {
    for {
      invalid@Invalid(_, _) <- validationManager.validateField(value, field.validationAnnotations)
    } yield {
      CaseClassValidationException(PropertyPath.leaf(field.name),  invalid)
    }
  }

  private def executeMethodValidations(fieldErrors: Seq[CaseClassValidationException], obj: Any) {
    val methodValidationErrors = for {
      invalid@Invalid(_, _) <- validationManager.validateObject(obj)
    } yield CaseClassValidationException(PropertyPath.empty, invalid)

    if (methodValidationErrors.nonEmpty) {
      throw new CaseClassMappingException(fieldErrors.toSet ++ methodValidationErrors.toSet)
    }
  }

  private def propertyNamingStrategy = {
    if (javaType.getRawClass.isAnnotationPresent(classOf[JsonCamelCase]))
      CamelCasePropertyNamingStrategy
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
