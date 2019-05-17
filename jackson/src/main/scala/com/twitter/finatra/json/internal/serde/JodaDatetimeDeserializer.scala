package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat
import com.fasterxml.jackson.datatype.joda.deser.JodaDateDeserializerBase
import com.twitter.finatra.json.internal.caseclass.exceptions.FinatraJsonMappingException
import com.twitter.util.{Return, Try}
import org.joda.time.{DateTime, DateTimeZone}

/**
 * A Datetime deserializer with improved exception handling (compared to jackson-datatype-joda)
 */
private[finatra] class JodaDatetimeDeserializer(format: JacksonJodaDateFormat)
    extends JodaDateDeserializerBase[DateTime](classOf[DateTime], format) {

  override def withFormat(jacksonJodaDateFormat: JacksonJodaDateFormat): JodaDatetimeDeserializer =
    new JodaDatetimeDeserializer(format)

  def deserialize(jp: JsonParser, ctxt: DeserializationContext): DateTime = {
    try {
      jp.getCurrentToken match {
        case JsonToken.VALUE_NUMBER_INT =>
          val value: Long = jp.getLongValue
          parseFromLong(ctxt, value)
        case JsonToken.VALUE_STRING =>
          val value: String = jp.getText.trim
          if (value.isEmpty) {
            throw new FinatraJsonMappingException("field cannot be empty")
          } else {
            // First, attempt to convert as a String value (for backwards-compatibility),
            // Long millis will fail and we then attempt to parse with value.toLong
            Try(this._format.createParser(ctxt).parseDateTime(value)) match {
              case Return(result) => result
              case _ => parseFromLong(ctxt, value.toLong)
            }
          }
        case _ =>
          this._handleNotNumberOrString(jp, ctxt)
      }
    } catch {
      case e: IllegalArgumentException =>
        val jme = new FinatraJsonMappingException(
          "error parsing '" + jp.getText + "' into an ISO 8601 datetime"
        )
        jme.addSuppressed(e)
        throw jme
    }
  }

  private def parseFromLong(ctxt: DeserializationContext, value: Long): DateTime = {
    if (value < 0) {
      throw new FinatraJsonMappingException("field cannot be negative")
    } else {
      val tz = if (this._format.isTimezoneExplicit) {
        this._format.getTimeZone
      } else {
        DateTimeZone.forTimeZone(ctxt.getTimeZone)
      }
      new DateTime(value, tz)
    }
  }
}
