package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonParser, ObjectCodec}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.TreeTraversingParser
import com.fasterxml.jackson.databind.util.ClassUtil
import com.twitter.finatra.json.internal.caseclass.exceptions.JsonFieldParseException
import com.twitter.finatra.json.internal.caseclass.reflection.CaseClassSigParser
import com.twitter.finatra.json.internal.caseclass.reflection.DefaultMethodUtils.defaultFunction
import com.twitter.finatra.json.internal.caseclass.utils.AnnotationUtils._
import com.twitter.finatra.json.internal.caseclass.utils.FieldInjection
import com.twitter.finatra.validation.Validation
import com.twitter.inject.Logging
import java.lang.annotation.Annotation
import scala.language.existentials
import scala.reflect.NameTransformer

object CaseClassField {

  def createFields(clazz: Class[_], namingStrategy: PropertyNamingStrategy, typeFactory: TypeFactory): Seq[CaseClassField] = {
    val allAnnotations = constructorAnnotations(clazz)
    val constructorParams = CaseClassSigParser.parseConstructorParams(clazz)
    assert(allAnnotations.size == constructorParams.size, "Non-static inner 'case classes' not supported")

    for {
      (constructorParam, idx) <- constructorParams.zipWithIndex
      annotations = allAnnotations(idx)
      name = jsonNameForField(annotations, namingStrategy, constructorParam.name)
      deserializer = deserializerOrNone(annotations)
    } yield {
      CaseClassField(
        name = name,
        javaType = JacksonTypes.javaType(typeFactory, constructorParam.scalaType),
        parentClass = clazz,
        defaultFuncOpt = defaultFunction(clazz, idx),
        annotations = annotations,
        deserializer = deserializer
      )
    }
  }

  private[finatra] def constructorAnnotations(clazz: Class[_]): Seq[Array[Annotation]] = {
    clazz.getConstructors.head.getParameterAnnotations.toSeq
  }

  private def jsonNameForField(annotations: Seq[Annotation], namingStrategy: PropertyNamingStrategy, name: String): String = {
    findAnnotation[JsonProperty](annotations) match {
      case Some(jsonProperty) => jsonProperty.value
      case _ =>
        val decodedName = NameTransformer.decode(name) //decode unicode escaped field names
        namingStrategy.nameForField(//apply json naming strategy (e.g. snake_case)
          /* config = */ null,
          /* field = */ null,
          /* defaultName = */ decodedName)
    }
  }

  private def deserializerOrNone(annotations: Array[Annotation]): Option[JsonDeserializer[_ <: Object]] = {
    findAnnotation[JsonDeserialize](annotations) match {
      case Some(jsonDeserializer) => {
        Some(ClassUtil.createInstance(jsonDeserializer.using, false).asInstanceOf[JsonDeserializer[_ <: Object]])
      }
      case _ => None
    }
  }
}

case class CaseClassField(
  name: String,
  javaType: JavaType,
  parentClass: Class[_],
  defaultFuncOpt: Option[() => Object],
  annotations: Seq[Annotation],
  deserializer: Option[JsonDeserializer[_ <: Object]])
  extends Logging {

  private val isOption = javaType.getRawClass == classOf[Option[_]]
  private val isString = javaType.getRawClass == classOf[String]
  private val fieldInjection = new FieldInjection(name, javaType, parentClass, annotations)
  private lazy val firstTypeParam = javaType.containedType(0)
  private lazy val requiredFieldException = JsonFieldParseException(name + " is a required field")

  /* Public */

  lazy val missingValue = {
    if (javaType.isPrimitive)
      ClassUtil.defaultValue(javaType.getRawClass)
    else
      null
  }

  val validationAnnotations =
    filterIfAnnotationPresent[Validation](annotations)


  /**
   * Parse the field from a JsonNode representing a JSON object
   * NOTE: I'd normally return a Try[Object], but instead I'm using exceptions to optimize the non-failure case
   * NOTE: Option fields default to None even if no default is specified
   *
   * @param context DeserializationContext for deserialization
   * @param jp JsonParser for field
   * @param objectJsonNode The JSON object
   * @return The parsed object for this field
   * @throws JsonFieldParseException with reason for the parsing error
   */
  def parse(context: DeserializationContext, jp: JsonParser, objectJsonNode: JsonNode): Object = {
    val codec = jp.getCodec
    if (fieldInjection.isInjectable)
      fieldInjection.inject(context, codec) orElse defaultValue getOrElse throwRequiredFieldException()
    else {
      val fieldJsonNode = objectJsonNode.get(name)
      if (deserializer.isDefined) {
        deserializer.get.deserialize(new TreeTraversingParser(fieldJsonNode, codec), context)
      } else {
        if (fieldJsonNode != null)
          if (isOption)
            Option(
              parseFieldValue(codec, fieldJsonNode, firstTypeParam))
          else
            assertNotNull(
              fieldJsonNode,
              parseFieldValue(codec, fieldJsonNode, javaType))
        else if (defaultFuncOpt.isDefined)
          defaultFuncOpt.get.apply()
        else if (isOption)
          None
        else
          throwRequiredFieldException()
      }
    }
  }

  /* Private */

  //optimized
  private[this] def parseFieldValue(fieldCodec: ObjectCodec, field: JsonNode, fieldType: JavaType): Object = {
    if (isString) {
      field.asText()
    }
    else {
      fieldCodec.readValue[Object](
        new TreeTraversingParser(field, fieldCodec),
        fieldType)
    }
  }

  //optimized
  private[this] def assertNotNull(field: JsonNode, value: Object): Object = {
    if (value == null) {
      throw new JsonMappingException("error parsing '" + field.asText + "'")
    }
    value
  }

  private def defaultValue: Option[Object] = {
    if (defaultFuncOpt.isDefined)
      defaultFuncOpt map {_()}
    else if (isOption)
      Some(None)
    else
      None
  }

  private def throwRequiredFieldException() = {
    throw requiredFieldException
  }
}
