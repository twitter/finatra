package com.fasterxml.jackson.databind

import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object ObjectMapperCopier {

  // Workaround since calling objectMapper.copy on "ObjectMapper with ScalaObjectMapper" fails the _checkInvalidCopy check
  def copy(objectMapper: ObjectMapper) = {
    new ObjectMapper(objectMapper) with ScalaObjectMapper
  }
}
