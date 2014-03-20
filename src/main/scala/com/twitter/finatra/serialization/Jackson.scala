package com.twitter.finatra.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{JacksonModule, DefaultScalaModule}

/**
 * 2014-03-19
 * @author Michael Rose <michael@fullcontact.com>
 */

object Jackson {
  implicit def jsonMapper:ObjectMapper  = {
    val m = new ObjectMapper()
    m.registerModule(DefaultScalaModule)
  }

  def registerModule(module:JacksonModule) {
    jsonMapper.registerModule(module)
  }
}