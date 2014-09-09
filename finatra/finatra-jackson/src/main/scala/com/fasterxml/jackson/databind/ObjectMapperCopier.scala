package com.fasterxml.jackson.databind

import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object ObjectMapperCopier {

  // Workaround for not being able to call objectMapper.copy on "ObjectMapper with ScalaObjectMapper"
  def copy(objectMapper: ObjectMapper) = {
    new ObjectMapper(objectMapper) with ScalaObjectMapper
  }
}
