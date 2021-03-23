package com.twitter.finatra.http.marshalling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.{JsonParser, JsonToken, ObjectCodec}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.node.TreeTraversingParser
import com.fasterxml.jackson.databind.{
  BeanProperty,
  DeserializationContext,
  JavaType,
  JsonDeserializer,
  JsonMappingException,
  JsonNode
}
import com.google.inject.Injector
import com.twitter.finagle.http.{Message, Request, Response}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.caseclass.exceptions.{
  CaseClassFieldMappingException,
  InjectableValuesException
}
import com.twitter.finatra.jackson.caseclass.{
  DefaultInjectableValues,
  Types,
  isNotAssignableFrom,
  newBeanProperty,
  resolveSubType
}
import com.twitter.finatra.json.annotations.InjectableValue
import com.twitter.finatra.http.annotations.{FormParam, Header, QueryParam, RouteParam}
import com.twitter.finatra.validation.ErrorCode
import com.twitter.finatra.validation.ValidationResult.Invalid
import com.twitter.util.reflect.Annotations
import java.lang.annotation.Annotation
import scala.collection.JavaConverters._

private object MessageInjectableValues {
  val SeqWithSingleEmptyString: Seq[String] = Seq("")

  val requestParamsAnnotations: Seq[Class[_ <: Annotation]] =
    Seq(classOf[RouteParam], classOf[QueryParam], classOf[FormParam])

  val annotations: Seq[Class[_ <: Annotation]] =
    Seq(classOf[RouteParam], classOf[QueryParam], classOf[FormParam], classOf[Header])

  private[http] class RepeatedCommaSeparatedQueryParameterException(fieldName: String)
      extends CaseClassFieldMappingException(
        null,
        Invalid(
          s"Repeating $fieldName is not allowed. Pass multiple values as a single comma-separated string.",
          ErrorCode.RepeatedCommaSeparatedCollection)
      )
}

private[http] class MessageInjectableValues(
  injector: Injector,
  objectMapper: ScalaObjectMapper,
  message: Message)
    extends DefaultInjectableValues(injector) {
  import MessageInjectableValues._

  /**
   * Lookup the key using the data in the Request object or objects in the object graph.
   *
   * @note this class uses nulls extensively as it is an integration with the Jackson
   *       java library.
   *
   * @param valueId Key for looking up the value
   * @param context DeserializationContext
   * @param forProperty BeanProperty
   * @param beanInstance Bean instance
   * @return the injected value
   */
  override final def findInjectableValue(
    valueId: Object,
    context: DeserializationContext,
    forProperty: BeanProperty,
    beanInstance: Object
  ): Object = message match {
    case request: Request if injector != null =>
      handle(
        valueId,
        context,
        forProperty,
        beanInstance,
        fieldNameForAnnotation(forProperty),
        request
      )
    case response: Response if injector != null =>
      handle(
        valueId,
        context,
        forProperty,
        beanInstance,
        fieldNameForAnnotation(forProperty),
        response
      )
    case _ =>
      null
  }

  /** Handle [[Request]] types */
  private[this] def handle(
    valueId: Object,
    context: DeserializationContext,
    forProperty: BeanProperty,
    beanInstance: Object,
    fieldName: String,
    request: Request
  ): Object = {
    try {
      if (isRequest(forProperty)) {
        request
      } else if (hasAnnotation(forProperty, requestParamsAnnotations)) {
        if (forProperty.getType.isCollectionLikeType) {
          request.params.getAll(fieldName) match {
            case propertyValue: Seq[String]
                if propertyValue.nonEmpty || request.params.contains(fieldName) =>
              val separatedValues = handleCommaSeparatedLists(forProperty, fieldName, propertyValue)
              val value = handleEmptySeq(forProperty, separatedValues)
              val modifiedParamsValue = handleExtendedBooleans(forProperty, value)
              convert(valueId, context, forProperty, modifiedParamsValue)
            case _ => null
          }
        } else {
          request.params.get(fieldName) match {
            case Some(value) =>
              val modifiedParamsValue = handleExtendedBooleans(forProperty, value)
              convert(valueId, context, forProperty, modifiedParamsValue)
            case _ => null
          }
        }
      } else if (forProperty.getContextAnnotation(classOf[Header]) != null) {
        getHeader(valueId, context, forProperty, fieldName, request)
      } else {
        // handle if the field is annotated with an injection annotation
        super.findInjectableValue(valueId, context, forProperty, beanInstance)
      }
    } catch {
      // Only translate `InvalidDefinitionException` to InjectableValuesException,
      // all others escape to be handled elsewhere.
      case ex: InvalidDefinitionException =>
        debug(ex.getMessage, ex)
        throw InjectableValuesException(
          forProperty.getMember.getDeclaringClass,
          forProperty.getName)
    }
  }

  /** Handle [[Response]] types */
  private[this] def handle(
    valueId: Object,
    context: DeserializationContext,
    forProperty: BeanProperty,
    beanInstance: Object,
    fieldName: String,
    response: Response
  ): Object = {
    try {
      if (isResponse(forProperty)) {
        response
      } else if (forProperty.getContextAnnotation(classOf[Header]) != null) {
        getHeader(valueId, context, forProperty, fieldName, response)
      } else if (hasAnnotation(forProperty, requestParamsAnnotations)) {
        // request annotations are not supported for parsing a response
        val message =
          s"Unable to inject field '$fieldName'. ${classOf[Request].getSimpleName}-specific " +
            s"annotations: [${requestParamsAnnotations.map(a => s"@${a.getSimpleName}").mkString(", ")}] " +
            s"are not supported with a ${classOf[Response].getName}."
        throw new InjectableValuesException(message)
      } else {
        // handle if the field is annotated with an injection annotation
        super.findInjectableValue(valueId, context, forProperty, beanInstance)
      }
    } catch {
      // Only translate `InvalidDefinitionException` to InjectableValuesException,
      // all others escape to be handled elsewhere.
      case ex: InvalidDefinitionException =>
        debug(ex.getMessage, ex)
        throw InjectableValuesException(
          forProperty.getMember.getDeclaringClass,
          forProperty.getName)
    }
  }

  /** Retrieve a Header value */
  private[this] def getHeader(
    valueId: Object,
    context: DeserializationContext,
    forProperty: BeanProperty,
    name: String,
    message: Message
  ): Object = {
    message.headerMap.get(name) match {
      case Some(p) => convert(valueId, context, forProperty, p)
      case None => null
    }
  }

  /** Try to convert */
  private[this] def convert(
    valueId: Object,
    context: DeserializationContext,
    forProperty: BeanProperty,
    propertyValue: Any
  ): Object = {
    if (forProperty.getType.hasRawClass(classOf[Option[_]])) {
      if (propertyValue == "") {
        None
      } else {
        Option(
          convert(
            valueId,
            context,
            forProperty,
            forProperty.getType.containedType(0),
            propertyValue))
      }
    } else if (forProperty.getType.hasRawClass(classOf[Boolean]) && propertyValue == "") {
      // for backwards compatibility: injected booleans with no value should
      // return null and not attempt conversion
      null
    } else {
      convert(valueId, context, forProperty, forProperty.getType, propertyValue)
    }
  }

  /** Convert based on a given [[JavaType]] */
  private[this] def convert(
    valueId: Object,
    context: DeserializationContext,
    forProperty: BeanProperty,
    javaType: JavaType,
    propertyValue: Any
  ): Object = {
    val withAnnotations = newBeanProperty(
      valueId,
      context,
      javaType = javaType,
      optionalJavaType = None,
      annotatedParameter = null,
      annotations = getAllAnnotations(context, forProperty.getMember, javaType),
      name = forProperty.getName,
      index = 0 // only one value
    )
    convertWithContext(context, withAnnotations, withAnnotations.getType, propertyValue)
  }

  /** Try to convert with context */
  private[this] def convertWithContext(
    context: DeserializationContext,
    forProperty: BeanProperty,
    javaType: JavaType,
    propertyValue: Any
  ): Object = {
    // not a primitive, not a string and starts with a START_OBJECT token
    def shouldParseAsObjectType(javaType: JavaType, json: String): Boolean = {
      !javaType.isPrimitive && !javaType.hasRawClass(classOf[String]) &&
      json.startsWith(JsonToken.START_OBJECT.asString)
    }

    val jsonNode: JsonNode = propertyValue match {
      case json: String if shouldParseAsObjectType(javaType, json) =>
        // only parse into a tree node if the incoming java type is not primitive or String type
        objectMapper.underlying.readTree(objectMapper.underlying.getFactory.createParser(json))
      case _ =>
        objectMapper.underlying.valueToTree[JsonNode](propertyValue)
    }
    val treeTraversingParser = new TreeTraversingParser(jsonNode, objectMapper.underlying)
    try {
      // advance the parser to the next token for deserialization via deserializer
      treeTraversingParser.nextToken
      Option(forProperty.getAnnotation(classOf[JsonDeserialize])) match {
        case Some(annotation: JsonDeserialize)
            if isNotAssignableFrom(annotation.using, classOf[JsonDeserializer.None]) =>
          // Jackson doesn't seem to properly find deserializers specified with `@JsonDeserialize`
          // unless they are contextual, so we manually lookup and instantiate.
          Option(context.deserializerInstance(forProperty.getMember, annotation.using)) match {
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
          readPropertyValue(
            context,
            treeTraversingParser,
            objectMapper.underlying,
            forProperty,
            propertyValue,
            Some(annotation.contentAs))
        case _ =>
          readPropertyValue(
            context,
            treeTraversingParser,
            objectMapper.underlying,
            forProperty,
            propertyValue,
            None)
      }
    } finally {
      treeTraversingParser.close()
    }
  }

  /** Finally, read the property value */
  private[this] def readPropertyValue(
    context: DeserializationContext,
    jsonParser: JsonParser,
    fieldCodec: ObjectCodec,
    forProperty: BeanProperty,
    propertyValue: Any,
    subTypeClazz: Option[Class[_]]
  ): Object = {
    val resolvedType = resolveSubType(context, forProperty.getType, subTypeClazz)
    if (resolvedType.isPrimitive || resolvedType.hasRawClass(classOf[String])) {
      // for backwards compatibility we use convert which ignores mangled primitives (returns null)
      objectMapper.convert(
        propertyValue,
        context.constructType(Types.wrapperType(resolvedType.getRawClass)))
    } else {
      // need to check the bean property and potentially the raw class:
      // in the case where the `@JsonTypeInfo` is on a field in the case class
      // being marshalled here, the annotation will not occur in the bean property
      // so we must also scan the raw class
      Option(forProperty.getAnnotation(classOf[JsonTypeInfo]))
        .orElse(Option(resolvedType.getRawClass.getAnnotation(classOf[JsonTypeInfo]))) match {
        case Some(_) =>
          // for polymorphic types we cannot contextualize
          context.readValue(jsonParser, resolvedType)
        case _ =>
          // this will properly resolve Jackson Annotations on the BeanProperty handling contextualization
          context.readPropertyValue(jsonParser, forProperty, resolvedType)
      }
    }
  }

  /** Finds all annotations from the [[AnnotatedMember]] and on the mix-in class */
  private[this] def getAllAnnotations(
    context: DeserializationContext,
    annotatedMember: AnnotatedMember,
    javaType: JavaType
  ): Iterable[Annotation] = {
    annotatedMember.getAllAnnotations.annotations.asScala ++
      findMixInAnnotations(context, javaType)
  }

  /** We only read the class-level annotations as we do not handle anything per-field of the injectable type in this class */
  private[this] def findMixInAnnotations(
    context: DeserializationContext,
    javaType: JavaType
  ): Iterable[Annotation] = {
    Option(context.getConfig.findMixInClassFor(javaType.getRawClass)) match {
      case Some(mixinClazz) if !mixinClazz.isPrimitive =>
        // we explicitly do not read a mix-in for a primitive type
        mixinClazz.getAnnotations.toIterable
      case _ =>
        Iterable.empty[Annotation]
    }
  }

  /** We do not try to also support any `@JsonProperty` annotation on the field */
  private[this] def fieldNameForAnnotation(forProperty: BeanProperty): String = {
    findAnnotation(forProperty, annotations) match {
      case Some(annotation) =>
        Annotations.getValueIfAnnotatedWith[InjectableValue](annotation) match {
          case Some(value) if value != null && value.nonEmpty => value
          case _ => forProperty.getName
        }
      case _ =>
        forProperty.getName
    }
  }

  private[this] def handleCommaSeparatedLists(
    forProperty: BeanProperty,
    fieldName: String,
    propertyValue: Seq[String]
  ): Seq[String] = {
    Option(forProperty.getContextAnnotation(classOf[QueryParam])) match {
      case Some(queryParam) if queryParam.commaSeparatedList =>
        if (propertyValue.size > 1) {
          throw new RepeatedCommaSeparatedQueryParameterException(fieldName)
        } else {
          propertyValue.flatMap(_.split(','))
        }
      case _ =>
        propertyValue
    }
  }

  private[this] def handleEmptySeq(forProperty: BeanProperty, propertyValue: Any): Any = {
    if (propertyValue == SeqWithSingleEmptyString &&
      forProperty.getType.containedType(0).getRawClass != classOf[String]) {
      // if a query param is set with an empty value we will get an empty seq of
      // string, yet the property type may not be string
      Seq.empty
    } else {
      propertyValue
    }
  }

  private[this] def handleExtendedBooleans(forProperty: BeanProperty, propertyValue: Any): Any = {
    if (forProperty.getContextAnnotation(classOf[QueryParam]) != null) {
      val forType = forProperty.getType
      if (isBoolean(forType.getRawClass)) {
        matchExtendedBooleans(propertyValue.asInstanceOf[String])
      } else if (isSeqOfBooleans(forType)) {
        propertyValue.asInstanceOf[Seq[String]].map(matchExtendedBooleans)
      } else {
        propertyValue
      }
    } else {
      propertyValue
    }
  }

  private[this] def matchExtendedBooleans(value: String): String = value match {
    case "t" | "T" | "1" => "true"
    case "f" | "F" | "0" => "false"
    case _ => value
  }

  // Note that the InjectableValue annotation information for the field is carried in
  // the beanProperty.getContextAnnotation field, not the beanProperty.getAnnotation
  // field, because the underlying AnnotatedMember does not carry non-Jackson annotations
  // thus we pass all annotation information as context annotations.
  // See: com.fasterxml.jackson.databind.introspect.AnnotationMap
  private[this] def findAnnotation(
    beanProperty: BeanProperty,
    annotations: Seq[Class[_ <: Annotation]]
  ): Option[Annotation] =
    annotations
      .find(beanProperty.getContextAnnotation(_) != null)
      .map(beanProperty.getContextAnnotation(_))

  private[this] lazy val hasAnnotation: (BeanProperty, Seq[Class[_ <: Annotation]]) => Boolean = {
    (beanProperty, annotations) =>
      annotations.exists(beanProperty.getContextAnnotation(_) != null)
  }

  private[this] lazy val isSeqOfBooleans: JavaType => Boolean = { forType =>
    forType.hasRawClass(classOf[Seq[_]]) && isBoolean(forType.containedType(0).getRawClass)
  }

  private[this] lazy val isRequest: BeanProperty => Boolean = { forProperty =>
    forProperty.getType.hasRawClass(classOf[Request])
  }

  private[this] lazy val isResponse: BeanProperty => Boolean = { forProperty =>
    forProperty.getType.hasRawClass(classOf[Response])
  }

  private[this] def isBoolean(clazz: Class[_]): Boolean = {
    // handle both java.lang.Boolean class and boolean primitive
    clazz == classOf[java.lang.Boolean] || clazz.getName == "boolean"
  }
}
