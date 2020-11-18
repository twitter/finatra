package com.twitter.finatra.http.exceptions

import scala.collection.mutable.ArrayBuffer

/**
 * Represents a collection of [[com.twitter.finatra.http.exceptions.ExceptionMapper]]s
 * which is an {{{Iterable[Manifest[ExceptionMapper[_]]]}}}.
 */
class ExceptionMapperCollection extends Iterable[Manifest[ExceptionMapper[_]]] {

  private[this] val manifests = ArrayBuffer[Manifest[ExceptionMapper[_]]]()

  /**
   * Add a [[com.twitter.finatra.http.exceptions.ExceptionMapper]] by type [[T]]
   * @tparam T - ExceptionMapper type T which should subclass [[com.twitter.finatra.http.exceptions.ExceptionMapper]]
   */
  def add[T <: ExceptionMapper[_]: Manifest]: Unit = {
    val m: Manifest[T] = manifest[T]
    manifests += m.asInstanceOf[Manifest[ExceptionMapper[_]]]
  }

  override def foreach[U](f: (Manifest[ExceptionMapper[_]]) => U): Unit = manifests.foreach(f)

  override def iterator: Iterator[Manifest[ExceptionMapper[_]]] = manifests.iterator
}
