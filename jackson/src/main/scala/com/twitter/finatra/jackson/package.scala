package com.twitter.finatra

import com.fasterxml.jackson.databind.{ObjectMapper => JacksonObjectMapper}
import com.fasterxml.jackson.module.scala.experimental.{ScalaObjectMapper => JacksonScalaObjectMapper}

package object jackson {
  type JacksonScalaObjectMapperType = JacksonObjectMapper with JacksonScalaObjectMapper
}
