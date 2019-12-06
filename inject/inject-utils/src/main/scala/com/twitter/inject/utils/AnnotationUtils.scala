package com.twitter.inject.utils

import com.twitter.util.Try
import java.lang.annotation.Annotation

/** Utility methods for dealing with [[java.lang.annotation.Annotation]] */
object AnnotationUtils {

  /**
   * Filter a list of annotations discriminated on whether the
   * annotation is itself annotated with the passed annotation.
   *
   * @param annotations the list of [[Annotation]] instances to filter.
   * @tparam A the type of the annotation by which to filter.
   *
   * @return the filtered list of matching annotations.
   */
  def filterIfAnnotationPresent[A <: Annotation: Manifest](
    annotations: Seq[Annotation]
  ): Seq[Annotation] = annotations.filter(isAnnotationPresent[A])

  /**
   * Filters a list of annotations by annotation type discriminated by the set of given annotations.
   * @param filterSet the Set of [[Annotation]] classes by which to filter.
   * @param annotations the list [[Annotation]] instances to filter.
   *
   * @return the filtered list of matching annotations.
   */
  def filterAnnotations(
    filterSet: Set[Class[_ <: Annotation]],
    annotations: Seq[Annotation]
  ): Seq[Annotation] = {
    annotations.filter { annotation =>
      filterSet.contains(annotation.annotationType)
    }
  }

  /**
   * Find an [[Annotation]] within a given list of annotations of the given target.
   * annotation type.
   * @param target the class of the [[Annotation]] to find.
   * @param annotations the list of [[Annotation]] instances to search.
   *
   * @return the matching [[Annotation]] instance if found, otherwise None.
   */
  def findAnnotation(
    target: Class[_ <: Annotation],
    annotations: Seq[Annotation]
  ): Option[Annotation] = annotations.find(_.annotationType() == target)

  /**
   * Find an [[Annotation]] within a given list of annotations annotated by the given type param.
   * @param annotations the list of [[Annotation]] instances to search.
   * @tparam A the type of the [[Annotation]] to find.
   *
   * @return the matching [[Annotation]] instance if found, otherwise None.
   */
  def findAnnotation[A <: Annotation: Manifest](annotations: Seq[Annotation]): Option[A] = {
    annotations.collectFirst {
      case annotation if annotationEquals[A](annotation) =>
        annotation.asInstanceOf[A]
    }
  }

  /**
   * Determines if the given [[Annotation]] has an annotation type of the given type param.
   * @param annotation the [[Annotation]] to match.
   * @tparam A the type to match against.
   *
   * @return true if the given [[Annotation]] is of type [[A]], false otherwise.
   */
  def annotationEquals[A <: Annotation: Manifest](annotation: Annotation): Boolean =
    annotation.annotationType() == manifest[A].runtimeClass.asInstanceOf[Class[A]]

  /**
   * Determines if the given [[Annotation]] is annotated by an [[Annotation]] of the given
   * type param.
   * @param annotation the [[Annotation]] to match.
   * @tparam A the type of the [[Annotation]] to determine if is annotated on the [[Annotation]].
   *
   * @return true if the given [[Annotation]] is annotated with an [[Annotation]] of type [[A]],
   *         false otherwise.
   */
  def isAnnotationPresent[A <: Annotation: Manifest](annotation: Annotation): Boolean =
    annotation.annotationType.isAnnotationPresent(manifest[A].runtimeClass.asInstanceOf[Class[A]])

  /**
   * Determines if the given [[A]] is annotated by an [[Annotation]] of the given
   * type param [[ToFindAnnotation]].
   *
   * @tparam ToFindAnnotation the [[Annotation]] to match.
   * @tparam A the type of the [[Annotation]] to determine if is annotated on the [[A]].
   *
   * @return true if the given [[Annotation]] is annotated with an [[Annotation]] of type [[ToFindAnnotation]],
   *         false otherwise.
   */
  def isAnnotationPresent[
    ToFindAnnotation <: Annotation: Manifest,
    A <: Annotation: Manifest
  ]: Boolean = {
    val annotationToFindClazz: Class[Annotation] =
      manifest[ToFindAnnotation]
        .runtimeClass
        .asInstanceOf[Class[Annotation]]
    val annotationsByTypeArray: Array[Annotation] =
      manifest[A]
        .runtimeClass
        .asInstanceOf[Class[A]]
        .getAnnotationsByType(annotationToFindClazz)
    annotationsByTypeArray != null && annotationsByTypeArray.nonEmpty
  }

  /**
   * Attempts to find annotations per `case class` field returning a mapping of field name to list
   * of any found annotations.
   *
   * @note IMPORTANT: this is only intended for Scala case classes as it only tries to find annotations
   *       on constructors of the given class.
   * @param clazz the `Class` to inspect. This should represent a Scala case class.
   * @param fields the list of case class fields.
   * @return a mapping of field name to list of annotations.
   *
   * @see [[https://docs.scala-lang.org/tour/case-classes.html Tour of Scala Case Classes]]
   */
  def findAnnotations(
    clazz: Class[_],
    fields: Seq[String]
  ): Map[String, Seq[Annotation]] = {
    // for case classes, the annotations are only visible on the constructor.
    val clazzConstructorAnnotations: Array[Array[Annotation]] =
      clazz.getConstructors.head.getParameterAnnotations

    // find case class field annotations
    val clazzAnnotations: Map[String, Seq[Annotation]] = (for {
      (field, index) <- fields.zipWithIndex
      fieldAnnotations = clazzConstructorAnnotations(index)
    } yield {
      field -> fieldAnnotations.toSeq
    }).toMap

    // find inherited annotations
    val inheritedAnnotations: Map[String, Seq[Annotation]] =
      findDeclaredMethodAnnotations(clazz, Map.empty[String, Seq[Annotation]])

    // Merge the two maps: if the same annotation for a given field occurs in both lists, we keep
    // the clazz annotation to in effect "override" what was specified by inheritance. That is, it
    // is not expected that annotations are ever additive (in the sense that you can configure a
    // single field through multiple declarations of the same annotation) but rather either-or.
    clazzAnnotations.map {
      case (field: String, annotations: Seq[Annotation]) =>
        val inherited: Seq[Annotation] =
          inheritedAnnotations.getOrElse(field, Nil)
        // want to prefer what is coming in from clazz annotations over inherited
        field -> mergeAnnotationLists(annotations, inherited)
    }
  }

  /**
   * Attempts to return the `value()` of an [[Annotation]] annotated by an [[Annotation]] of
   * type [[A]].
   *
   * If the given [[Annotation]] is not annotated by an [[Annotation]] of type [[A]] or if the
   * `Annotation#value` method cannot be invoked then None is returned.
   *
   * @param annotation the [[Annotation]] to process.
   * @tparam A the [[Annotation]] type to match against before trying to invoke `annotation#value`.
   * @return the return of invoking `annotation#value()` or None.
   */
  private[twitter] def getValueIfAnnotatedWith[A <: Annotation: Manifest](
    annotation: Annotation
  ): Option[String] = {
    if (AnnotationUtils.isAnnotationPresent[A](annotation)) {
      for {
        method <- annotation.getClass.getDeclaredMethods.find(_.getName == "value")
        value <- Try(method.invoke(annotation).asInstanceOf[String]).toOption
      } yield value
    } else None
  }

  private[this] def findDeclaredMethodAnnotations(
    clazz: Class[_],
    found: Map[String, Seq[Annotation]]
  ): Map[String, Seq[Annotation]] = {
    // clazz declared method annotations
    val interfaceDeclaredAnnotations: Map[String, Seq[Annotation]] =
      clazz.getDeclaredMethods
        .map { method =>
          method.getName -> method.getDeclaredAnnotations.toSeq
        }.toMap.map {
        case (key, values) =>
          key -> mergeAnnotationLists(values, found.getOrElse(key, Seq.empty[Annotation]))
      }

    // interface declared method annotations
    clazz.getInterfaces.foldLeft(interfaceDeclaredAnnotations) {
      (acc: Map[String, Seq[Annotation]], interface: Class[_]) =>
        acc.map {
          case (key, values) =>
            key -> mergeAnnotationLists(
              values,
              findDeclaredMethodAnnotations(interface, acc).getOrElse(key, Seq.empty[Annotation]))
        }
    }
  }

  /** Prefer values in A over B */
  private[this] def mergeAnnotationLists(
    a: Seq[Annotation],
    b: Seq[Annotation]
  ): Seq[Annotation] = {
    a ++ b.filterNot(bAnnotation => a.exists(_.annotationType() == bAnnotation.annotationType()))
  }
}
