package com.twitter.finatra.conversions

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.json.FinatraObjectMapper
import java.io.OutputStream

object json {

  private val mapper = FinatraObjectMapper.create()

  class RichAny(any: Any) {

    def toJson: String = {
      mapper.writeValueAsString(any)
    }

    def toJsonBytes: Array[Byte] = {
      mapper.writeValueAsBytes(any)
    }

    def toJson(outputStream: OutputStream) {
      mapper.writeValue(any, outputStream)
    }

    def toPrettyJson: String = any match {
      case str: String =>
        mapper.writePrettyString(
          mapper.parse[JsonNode](str))
      case _ =>
        mapper.writePrettyString(any)
    }

    def toPrettyJsonStdout() {
      println(toPrettyJson)
    }

    def parseJson[T: Manifest]: T = {
      any match {
        case str: String =>
          mapper.parse(str)
        case bytes: Array[Byte] =>
          mapper.parse(bytes)
        case _ =>
          throw new Exception(any + " not yet supprted")
      }
    }
  }

  implicit def richAny(any: Any) = new RichAny(any)
}
