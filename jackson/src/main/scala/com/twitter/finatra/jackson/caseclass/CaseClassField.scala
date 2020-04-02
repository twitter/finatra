package com.twitter.finatra.jackson.caseclass

import com.fasterxml.jackson.annotation.{
  JsonIgnore,
  JsonIgnoreType,
  JsonProperty,
  JsonTypeInfo,
  JsonView
}
import com.fasterxml.jackson.core.{JsonParser, ObjectCodec}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import com.fasterxml.jackson.databind.node.TreeTraversingParser
import com.fasterxml.jackson.databind.util.ClassUtil
import com.twitter.conversions.StringOps._
import com.twitter.finatra.jackson.caseclass.exceptions.CaseClassFieldMappingException
import com.twitter.finatra.json.annotations.InjectableValue
import com.twitter.finatra.validation.ErrorCode
import com.twitter.finatra.validation.ValidationResult.Invalid
import com.twitter.inject.Logging
import com.twitter.inject.utils.AnnotationUtils
import java.lang.annotation.Annotation
import java.lang.reflect.Executable
import org.json4s.reflect.ClassDescriptor
import scala.annotation.tailrec
import scala.reflect.NameTransformer

/* Keeps a mapping of field "type" as a String and the field name */
private case class FieldInfo(`type`: String, fieldName: String)

object CaseClassField {

  // optimized
  private[jackson] def createFields(
    clazz: Class[_],
    constructor: Executable,
    clazzDescriptor: ClassDescriptor,
    propertyDefinitions: Array[PropertyDefinition],
    fieldAnnotations: scala.collection.Map[String, Array[Annotation]],
    namingStrategy: PropertyNamingStrategy,
    typeFactory: TypeFactory,
    injectableTypes: InjectableTypes
  ): Array[CaseClassField] = {
    /* CaseClassFields MUST be returned in constructor/method parameter invocation order */
    // we could choose to use the size of the property definitions here, but since they are
    // computed there is the possibility it is incorrect and thus we want to ensure we have
    // as many definitions as there are parameters defined for the given constructor.
    val parameters = constructor.getParameters
    val fields = new Array[CaseClassField](parameters.length)

    var index = 0
    while (index < parameters.length) {
      val propertyDefinition = propertyDefinitions(index)
      // we look up annotations by the field name as that is how they are keyed
      val annotations: Array[Annotation] =
        fieldAnnotations.get(propertyDefinition.beanPropertyDefinition.getName) match {
          case Some(v) => v
          case _ => Array.empty[Annotation]
        }
      val name = jsonNameForField(
        namingStrategy,
        clazz,
        annotations,
        propertyDefinition.beanPropertyDefinition.getName)

      fields(index) = CaseClassField(
        name = name,
        index = index,
        javaType = propertyDefinition.javaType,
        parentClass = clazz,
        annotations = annotations,
        beanPropertyDefinition = propertyDefinition.beanPropertyDefinition,
        defaultFn = defaultMethod(clazzDescriptor, index),
        injectableTypes = injectableTypes
      )

      index += 1
    }

    fields
  }

  private[this] def jsonNameForField(
    namingStrategy: PropertyNamingStrategy,
    clazz: Class[_],
    annotations: Array[Annotation],
    name: String
  ): String = {
    AnnotationUtils.findAnnotation[JsonProperty](annotations) match {
      case Some(jsonProperty) if jsonProperty.value.nonEmpty =>
        jsonProperty.value
      case _ =>
        val decodedName = NameTransformer.decode(name) // decode unicode escaped field names
        namingStrategy.nameForField( // apply json naming strategy (e.g. snake_case)
          /* config = */ null,
          /* field = */ null,
          /* defaultName = */ decodedName)
    }
  }
}

/* exposed for testing */
private[jackson] case class CaseClassField private (
  name: String,
  index: Int,
  javaType: JavaType,
  parentClass: Class[_],
  annotations: Array[Annotation],
  beanPropertyDefinition: BeanPropertyDefinition,
  defaultFn: Option[() => Object],
  injectableTypes: InjectableTypes)
    extends Logging {

  lazy val missingValue: AnyRef = {
    if (javaType.isPrimitive)
      ClassUtil.defaultValue(javaType.getRawClass)
    else
      null
  }

  private[this] val isOption = javaType.hasRawClass(classOf[Option[_]])
  private[this] val isString = javaType.hasRawClass(classOf[String])
  private[this] val fieldInjection =
    new FieldInjection(
      name,
      javaType,
      parentClass,
      annotations,
      injectableTypes
    )

  /** Lazy as we may not have a contained type */
  private[this] lazy val firstTypeParam: JavaType = javaType.containedType(0)

  /** Annotations defined on the case class definition */
  private[this] lazy val clazzAnnotations: Array[Annotation] =
    if (isOption) {
      javaType.containedType(0).getRawClass.getAnnotations
    } else {
      javaType.getRawClass.getAnnotations
    }

  /** If the field has a [[JsonIgnore(true)]] annotation or the type has been annotated with [[JsonIgnoreType(true)]]*/
  private[this] lazy val isIgnored: Boolean =
    AnnotationUtils
      .findAnnotation[JsonIgnore](annotations).exists(_.value) ||
      AnnotationUtils
        .findAnnotation[JsonIgnoreType](clazzAnnotations).exists(_.value)

  /** If the field is annotated directly with [[JsonDeserialize]] of if the class is annotated similarly */
  private[this] lazy val jsonDeserializer: Option[JsonDeserialize] =
    AnnotationUtils
      .findAnnotation[JsonDeserialize](annotations)
      .orElse(AnnotationUtils.findAnnotation[JsonDeserialize](clazzAnnotations))

  /* Public */
  /**
   * Parse the field from a JsonNode representing a JSON object. NOTE: We would normally return a
   * `Try[Object]`, but instead we use exceptions to optimize the non-failure case.
   *
   * @param context        DeserializationContext for deserialization
   * @param codec          Codec for field
   * @param objectJsonNode The JSON object
   *
   * @return The parsed object for this field
   * @note Option fields default to None even if no default is specified
   *
   * @throws CaseClassFieldMappingException with reason for the parsing error
   */
  def parse(
    context: DeserializationContext,
    codec: ObjectCodec,
    objectJsonNode: JsonNode
  ): Object = {
    val forProperty: BeanProperty = beanProperty(context)
    // current active view (if any)
    val activeJsonView: Option[Class[_]] = Option(context.getActiveView)
    // current @JsonView annotation (if any)
    val fieldJsonViews: Option[Seq[Class[_]]] =
      AnnotationUtils.findAnnotation[JsonView](annotations).map(_.value.toSeq)

    if (fieldInjection.isInjectable) {
      fieldInjection
        .findInjectableValue(context, codec, forProperty)
        .orElse(defaultValue)
        .getOrElse(throwRequiredFieldException(false)) // we ignore jackson annotations
    } else {
      // context has an active view *and* the field is annotated
      if (activeJsonView.isDefined && fieldJsonViews.isDefined) {
        if (activeJsonView.exists(fieldJsonViews.get.contains(_))) {
          // active view is in the list of views from the annotation
          parse(context, codec, objectJsonNode, forProperty)
        } else defaultValueOrException(isIgnored)
      } else {
        // no active view proceed as normal
        parse(context, codec, objectJsonNode, forProperty)
      }
    }
  }

  /* Private */

  private[this] def beanProperty(
    context: DeserializationContext,
    optionalJavaType: Option[JavaType] = None
  ): BeanProperty =
    newBeanProperty(
      valueId = fieldInjection.valueId,
      context = context,
      javaType = javaType,
      optionalJavaType = optionalJavaType,
      annotatedParameter = beanPropertyDefinition.getConstructorParameter,
      annotations = annotations,
      name = name,
      index = index
    )

  private[this] def parse(
    context: DeserializationContext,
    codec: ObjectCodec,
    objectJsonNode: JsonNode,
    forProperty: BeanProperty
  ): Object = {
    val fieldJsonNode = objectJsonNode.get(name)
    if (!isIgnored && fieldJsonNode != null && !fieldJsonNode.isNull) {
      if (isOption) {
        Option(
          parseFieldValue(
            context,
            codec,
            fieldJsonNode,
            beanProperty(context, Some(firstTypeParam))))
      } else {
        assertNotNull(
          context,
          fieldJsonNode,
          parseFieldValue(context, codec, fieldJsonNode, forProperty))
      }
    } else defaultValueOrException(isIgnored)
  }

  //optimized
  private def parseFieldValue(
    context: DeserializationContext,
    fieldCodec: ObjectCodec,
    field: JsonNode,
    forProperty: BeanProperty
  ): Object = {
    if (isString) {
      field.asText
    } else {
      val treeTraversingParser = new TreeTraversingParser(field, fieldCodec)
      try {
        // advance the parser to the next token for deserialization
        treeTraversingParser.nextToken
        jsonDeserializer match {
          case Some(annotation: JsonDeserialize)
              if isNotAssignableFrom(annotation.using, classOf[JsonDeserializer.None]) =>
            // Jackson doesn't seem to properly find deserializers specified with `@JsonDeserialize`
            // unless they are contextual, so we manually lookup and instantiate.
            Option(
              context.deserializerInstance(
                beanPropertyDefinition.getPrimaryMember,
                annotation.using)) match {
              case Some(deserializer) =>
                deserializer.deserialize(treeTraversingParser, context)
              case _ =>
                context.handleInstantiationProblem(
                  javaType.getRawClass,
                  annotation.using.toString,
                  JsonMappingException.from(
                    context,
                    "Unable to locate/create deserializer specified by: " +
                      s"${annotation.getClass.getName}(using = ${annotation.using()})")
                )
            }
          case Some(annotation: JsonDeserialize)
              if isNotAssignableFrom(annotation.contentAs, classOf[java.lang.Void]) =>
            parseFieldValue(
              context,
              fieldCodec,
              treeTraversingParser,
              forProperty,
              Some(annotation.contentAs))
          case _ =>
            parseFieldValue(
              context,
              fieldCodec,
              treeTraversingParser,
              forProperty,
              None
            )
        }
      } finally {
        treeTraversingParser.close()
      }
    }
  }

  private def parseFieldValue(
    context: DeserializationContext,
    fieldCodec: ObjectCodec,
    jsonParser: JsonParser,
    forProperty: BeanProperty,
    subTypeClazz: Option[Class[_]]
  ): Object = {
    val resolvedType = resolveSubType(context, forProperty.getType, subTypeClazz)
    AnnotationUtils.findAnnotation[JsonTypeInfo](clazzAnnotations) match {
      case Some(_) =>
        // for polymorphic types we cannot contextualize
        // thus we go back to the field codec to read
        fieldCodec.readValue(jsonParser, resolvedType)
      case _ if resolvedType.isContainerType =>
        // nor container types -- trying to contextualize on a container type leads to really bad performance
        // thus we go back to the field codec to read
        fieldCodec.readValue(jsonParser, resolvedType)
      case _ =>
        // contextualization for all others
        context.readPropertyValue(jsonParser, forProperty, resolvedType)
    }
  }

  //optimized
  private[this] def assertNotNull(
    context: DeserializationContext,
    field: JsonNode,
    value: Object
  ): Object = {
    value match {
      case null =>
        throw JsonMappingException.from(context, "error parsing '" + field.asText + "'")
      case traversable: Traversable[_] =>
        assertNotNull(context, traversable)
      case array: Array[_] =>
        assertNotNull(context, array)
      case _ => // no-op
    }
    value
  }

  private[this] def assertNotNull(
    context: DeserializationContext,
    traversable: Traversable[_]
  ): Unit = {
    if (traversable.exists(_ == null)) {
      throw JsonMappingException.from(
        context,
        "Literal null values are not allowed as json array elements."
      )
    }
  }

  /** Return the default value of the field */
  private[this] def defaultValue: Option[Object] = {
    if (defaultFn.isDefined)
      defaultFn.map(_())
    else if (isOption)
      Some(None)
    else
      None
  }

  /** Return the [[defaultValue]] or else throw a required field exception */
  private[this] def defaultValueOrException(ignoredField: Boolean): Object = {
    defaultValue match {
      case Some(value) => value
      case _ => throwRequiredFieldException(ignoredField)
    }
  }

  /** Throw a required field exception */
  private[this] def throwRequiredFieldException(ignorable: Boolean): Nothing = {
    val (fieldInfoAttributeType, fieldInfoAttributeName) = getFieldInfo(name, annotations)

    val message =
      if (ignorable) s"ignored $fieldInfoAttributeType has no default value specified"
      else s"$fieldInfoAttributeType is required"
    throw CaseClassFieldMappingException(
      CaseClassFieldMappingException.PropertyPath.leaf(fieldInfoAttributeName),
      Invalid(message, ErrorCode.RequiredFieldMissing)
    )
  }

  @tailrec
  private[this] def getFieldInfo(
    fieldName: String,
    annotations: Seq[Annotation]
  ): (String, String) = {
    if (annotations.isEmpty) {
      ("field", fieldName)
    } else {
      extractFieldInfo(fieldName, annotations.head) match {
        case Some(found) => found
        case _ => getFieldInfo(fieldName, annotations.tail)
      }
    }
  }

  // Annotations annotated with `@InjectableValue` represent values that are injected
  // into a constructed case class from somewhere other than the incoming JSON,
  // e.g., with Jackson InjectableValues. When we are parsing the structure of the
  // case class, we want to capture information about these annotations for proper
  // error reporting on the fields annotated. The annotation name is used instead
  // of the generic classifier of "field", and the annotation#value() is used
  // instead of the case class field name being marshalled into.
  private[this] def extractFieldInfo(
    fieldName: String,
    annotation: Annotation
  ): Option[(String, String)] = {
    if (AnnotationUtils.isAnnotationPresent[InjectableValue](annotation)) {
      val name = AnnotationUtils.getValueIfAnnotatedWith[InjectableValue](annotation) match {
        case Some(value) if value != null && value.nonEmpty => value
        case _ => fieldName
      }
      Some((annotation.annotationType.getSimpleName.toCamelCase, name))
    } else None
  }
}
