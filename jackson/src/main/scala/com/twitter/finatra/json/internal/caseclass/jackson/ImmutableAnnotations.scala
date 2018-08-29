package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.databind.util.Annotations
import com.twitter.inject.conversions.seq._
import java.lang.annotation.Annotation

private[json] case class ImmutableAnnotations(annotations: Seq[Annotation]) extends Annotations {

  private val annotationsMap: Map[Class[_ <: Annotation], Annotation] =
    annotations.groupBySingleValue(_.annotationType)

  override def get[A <: Annotation](cls: Class[A]): A = {
    annotationsMap.get(cls).orNull.asInstanceOf[A]
  }

  override def size(): Int = {
    annotationsMap.size
  }

  override def has(clazz: Class[_]): Boolean = {
    annotationsMap.get(clazz.asInstanceOf[Class[_ <: Annotation]]) != null
  }

  override def hasOneOf(clazzes: Array[Class[_ <: Annotation]]): Boolean = {
    var i = 0
    while (i < clazzes.length) {
      if (has(clazzes(i))) {
        return true
      }
      i += 1
    }
    false
  }
}
