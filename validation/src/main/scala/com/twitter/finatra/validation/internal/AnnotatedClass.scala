package com.twitter.finatra.validation.internal

import com.twitter.finatra.validation.Path
import java.lang.annotation.Annotation
import org.json4s.reflect.ScalaType

/**
 * A Finatra internal class that carries all field and method annotations of the given case class.
 *
 * [[scalaType]] represents the class or its binding, e.g. `T` or `Option[T]`
 */
private[validation] case class AnnotatedClass(
  name: Option[String],
  path: Path,
  clazz: Class[_],
  scalaType: Option[ScalaType],
  members: Array[AnnotatedMember],
  methods: Array[AnnotatedMethod])
    extends AnnotatedMember {

  // finds members by the given name. this may return
  // multiple as there may be an AnnotatedField and
  // and an AnnotatedClass for the same field name
  def findAnnotatedMembersByName(
    name: String,
    acc: scala.collection.mutable.ArrayBuffer[AnnotatedMember] =
      scala.collection.mutable.ArrayBuffer.empty
  ): Array[AnnotatedMember] = {
    val collectedAnnotatedMembers = members ++ methods
    var i = 0
    while (i < collectedAnnotatedMembers.length) {
      val member = collectedAnnotatedMembers(i)
      member match {
        case field: AnnotatedField =>
          if (field.name.contains(name)) acc.append(field)
        case method: AnnotatedMethod =>
          if (method.name.contains(name)) acc.append(method)
        case clazz: AnnotatedClass =>
          if (clazz.name.contains(name)) acc.append(clazz)
          clazz.findAnnotatedMembersByName(name, acc)
      }
      i += 1
    }

    acc.toArray
  }

  def getAnnotationsForAnnotatedMember(name: String): Array[Annotation] = {
    findAnnotatedMembersByName(name).flatMap(getAnnotationForAnnotatedMember)
  }

  private[this] def getAnnotationForAnnotatedMember(
    annotatedMember: AnnotatedMember
  ): Array[Annotation] =
    annotatedMember match {
      case f: AnnotatedField =>
        f.fieldValidators.map(_.annotation)
      case c: AnnotatedClass =>
        c.members.flatMap(getAnnotationForAnnotatedMember)
      case m: AnnotatedMethod =>
        Array(m.annotation)
    }
}
