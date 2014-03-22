package com.twitter.finatra.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.jboss.netty.util.CharsetUtil._

/**
 * 2014-03-20
 * @author Michael Rose <michael@fullcontact.com>
 */

trait JsonSerializer {
  def serialize[T](item: T): Array[Byte]
}

object JsonSerializer {
  implicit object JacksonSerializer extends JsonSerializer{
    var mapper = {
      new ObjectMapper().registerModule(new DefaultScalaModule)
    }

    def serialize[T](item: T) = {
      mapper.writeValueAsString(item).getBytes(UTF_8)
    }
  }
}

