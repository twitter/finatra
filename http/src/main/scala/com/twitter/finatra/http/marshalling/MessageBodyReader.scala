package com.twitter.finatra.http.marshalling

import com.fasterxml.jackson.databind.ObjectReader
import com.twitter.finagle.http.Message

object MessageBodyReader {

  /**
   * Simple utility for direct parsing of [[Message]] body into an object
   * using the given [[ObjectReader]].
   *
   * @note this method assumes the given `reader` is configured to understand how to
   *       parse the incoming message type, e.g., that it is correctly configured with any
   *       additional request-specific [[com.fasterxml.jackson.databind.InjectableValues]].
   *       That is, this method explicitly *does not* provide any request-specific value
   *       injection but relies directly on the configuration of the given.
   *
   * @param message the [[Message]] to parse.
   * @param reader the configured [[ObjectReader]] to use to parse the given [[Message]] body.
   * @tparam T the type of the parsed return instance.
   */
  private[finatra] def parseMessageBody[T: Manifest](message: Message, reader: ObjectReader): T = {
    val inputStream = message.getInputStream()
    try {
      reader.readValue[T](inputStream)
    } finally {
      inputStream.close()
    }
  }
}

/**
 * Transforms an HTTP Message into an object. Usable from Java.
 * @note Scala users please use the [[MessageBodyReader]] trait.
 */
abstract class AbstractMessageBodyReader[T] extends MessageBodyReader[T]

/**
 * Transforms an HTTP Message into an object.
 * @note Java users should prefer the [[AbstractMessageBodyReader]] abstract class.
 */
trait MessageBodyReader[T] extends MessageBodyComponent {
  def parse(message: Message): T
}
