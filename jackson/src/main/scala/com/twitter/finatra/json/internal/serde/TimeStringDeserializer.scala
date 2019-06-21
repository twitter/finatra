package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind.{BeanProperty, DeserializationContext, JsonDeserializer}
import com.twitter.util.{Time, TimeFormat}
import java.util.{Locale, TimeZone}

private[finatra] object TimeStringDeserializer {
  private[this] val DefaultTimeFormat: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  def apply(): TimeStringDeserializer = this(DefaultTimeFormat)

  def apply(pattern: String): TimeStringDeserializer =
    this(pattern, None, TimeZone.getTimeZone("UTC"))

  def apply(pattern: String, locale: Option[Locale], timezone: TimeZone): TimeStringDeserializer =
    new TimeStringDeserializer(new TimeFormat(pattern, locale, timezone))
}

private[finatra] class TimeStringDeserializer(
  private[this] val timeFormat: TimeFormat
) extends StdScalarDeserializer[Time](classOf[Time]) with ContextualDeserializer {

  override def deserialize(parser: JsonParser, context: DeserializationContext): Time = {
    timeFormat.parse(parser.getValueAsString)
  }

  /**
   * This method allows extracting the [[com.fasterxml.jackson.annotation.JsonFormat JsonFormat]]
   * annotation and create a [[com.twitter.util.TimeFormat TimeFormat]] based on the specifications
   * provided in the annotation. The implementation follows the Jackson's java8 & joda-time versions
   *
   * @param context  Deserialization context to access configuration, additional deserializers
   *                 that may be needed by this deserializer
   * @param property Method, field or constructor parameter that represents the property (and is
   *                 used to assign deserialized value). Should be available; but there may be
   *                 cases where caller can not provide it and null is passed instead
   *                 (in which case impls usually pass 'this' deserializer as is)
   *
   * @return Deserializer to use for deserializing values of specified property; may be this
   *         instance or a new instance.
   *
   * @see https://github.com/FasterXML/jackson-modules-java8/blob/master/datetime/src/main/java/com/fasterxml/jackson/datatype/jsr310/deser/JSR310DateTimeDeserializerBase.java#L29
   */
  override def createContextual(
    context: DeserializationContext,
    property: BeanProperty
  ): JsonDeserializer[_] = {
    val deserializerOption: Option[TimeStringDeserializer] = for {
      jsonFormat <- Option(findFormatOverrides(context, property, handledType()))
      deserializer <- Option(TimeStringDeserializer(
        jsonFormat.getPattern,
        Option(jsonFormat.getLocale),
        Option(jsonFormat.getTimeZone).getOrElse(TimeZone.getTimeZone("UTC"))
      )) if jsonFormat.hasPattern
    } yield {
      deserializer
    }
    deserializerOption.getOrElse(this)
  }
}
