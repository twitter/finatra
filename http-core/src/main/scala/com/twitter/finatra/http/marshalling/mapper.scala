package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Message
import com.twitter.util.jackson.ScalaObjectMapper

object mapper {

  implicit class RichObjectMapper[M <: ScalaObjectMapper](val self: M) extends AnyVal {
    def parseMessageBody[T: Manifest](message: Message): T = {
      if (message.isRequest) {
        val length = message.contentLength.getOrElse(0L)
        if (length == 0) {
          throw new UnsupportedOperationException(
            "Injecting request attributes (e.g. QueryParam, Header, etc) not supported when explicitly calling " +
              "ScalaObjectMapper.parse. Instead use a 'case class' input parameter on a Controller callback " +
              "(e.g. get('/') { r: ClassWithRequestAttributes => ... } ).")
        }
      }
      MessageBodyReader.parseMessageBody(message, self.reader[T])
    }
  }
}
