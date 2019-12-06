package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Message

/**
 * Transforms an object into an HTTP Response. Usable from Java.
 *
 * @note This allows Java users to not have to implement the base `#write(Message, T)`
 *       unless necessary as a defaulted implementation exists in the trait but needs
 *       to be accessible to Java users as an abstract class.
 * @note Scala users please use the [[MessageBodyWriter]] trait.
 */
abstract class AbstractMessageBodyWriter[T] extends MessageBodyWriter[T]

/**
 * Transforms an object into an HTTP Response.
 * @note Java users should prefer the [[AbstractMessageBodyWriter]] abstract class.
 */
trait MessageBodyWriter[T] extends MessageBodyComponent {
  def write(message: Message, obj: T): WriterResponse = write(obj)
  def write(obj: T): WriterResponse
}
