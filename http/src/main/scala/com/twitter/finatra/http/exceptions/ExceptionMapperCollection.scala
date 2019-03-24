package com.twitter.finatra.http.exceptions

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe.TypeTag

/**
 * Represents a collection of [[com.twitter.finatra.http.exceptions.ExceptionMapper]]s
 * which is a Traversable[TypeTag[ExceptionMapper[_\]\]\].
 */
class ExceptionMapperCollection extends Traversable[TypeTag[ExceptionMapper[_]]] {

  private[this] val typeTags = ArrayBuffer[TypeTag[ExceptionMapper[_]]]()

  /**
   * Add a [[com.twitter.finatra.http.exceptions.ExceptionMapper]] by type [[T]]
   * @tparam T - ExceptionMapper type T which should subclass [[com.twitter.finatra.http.exceptions.ExceptionMapper]]
   */
  def add[T <: ExceptionMapper[_]: TypeTag]: Unit = {
    val typeTag: TypeTag[T] = implicitly[TypeTag[T]]
    typeTags += typeTag.asInstanceOf[TypeTag[ExceptionMapper[_]]]
  }

  override def foreach[U](f: (TypeTag[ExceptionMapper[_]]) => U): Unit = typeTags.foreach(f)

  override def seq: Traversable[TypeTag[ExceptionMapper[_]]] = typeTags.seq
}
