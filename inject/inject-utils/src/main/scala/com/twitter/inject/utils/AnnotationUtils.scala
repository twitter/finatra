package com.twitter.inject.utils

import com.twitter.util.{Return, Try}
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
    annotations: Array[Annotation]
  ): Array[Annotation] = annotations.filter(isAnnotationPresent[A])

  /**
   * Filters a list of annotations by annotation type discriminated by the set of given annotations.
   * @param filterSet the Set of [[Annotation]] classes by which to filter.
   * @param annotations the list [[Annotation]] instances to filter.
   *
   * @return the filtered list of matching annotations.
   */
  def filterAnnotations(
    filterSet: Set[Class[_ <: Annotation]],
    annotations: Array[Annotation]
  ): Array[Annotation] =
    annotations.filter(a => filterSet.contains(a.annotationType))

  /**
   * Find an [[Annotation]] within a given list of annotations of the given target.
   * annotation type.
   * @param target the class of the [[Annotation]] to find.
   * @param annotations the list of [[Annotation]] instances to search.
   *
   * @return the matching [[Annotation]] instance if found, otherwise None.
   */
  // optimized
  def findAnnotation(
    target: Class[_ <: Annotation],
    annotations: Array[Annotation]
  ): Option[Annotation] = {
    var found: Option[Annotation] = None
    var index = 0
    while (index < annotations.length && found.isEmpty) {
      val annotation = annotations(index)
      if (annotation.annotationType() == target) found = Some(annotation)
      index += 1
    }
    found
  }

  /**
   * Find an [[Annotation]] within a given list of annotations annotated by the given type param.
   * @param annotations the list of [[Annotation]] instances to search.
   * @tparam A the type of the [[Annotation]] to find.
   *
   * @return the matching [[Annotation]] instance if found, otherwise None.
   */
  // optimized
  def findAnnotation[A <: Annotation: Manifest](annotations: Array[Annotation]): Option[A] = {
    val size = annotations.length
    val annotationType = manifest[A].runtimeClass.asInstanceOf[Class[A]]
    var found: Option[A] = None
    var index = 0
    while (found.isEmpty && index < size) {
      val annotation = annotations(index)
      if (annotation.annotationType() == annotationType) found = Some(annotation.asInstanceOf[A])
      index += 1
    }
    found
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
      manifest[ToFindAnnotation].runtimeClass
        .asInstanceOf[Class[Annotation]]

    manifest[A].runtimeClass
      .asInstanceOf[Class[A]].isAnnotationPresent(annotationToFindClazz)
  }

  /**
   * Attempts to find annotations per `case class` field returning a mapping of field name to list
   * of any found annotations.
   *
   * @note IMPORTANT: this is only intended for Scala case classes as it only tries to find annotations
   *       on constructors of the given class.
   * @param clazz the `Class` to inspect. This should represent a Scala case class.
   * @param fields the list of case class fields.
   * @return a mapping of field name to list of annotations. Note, this only returns fields which have
   *         annotations.
   *
   * @see [[https://docs.scala-lang.org/tour/case-classes.html Tour of Scala Case Classes]]
   */
  // optimized
  @deprecated("No public replacement", "2020-09-22")
  def findAnnotations(
    clazz: Class[_],
    fields: Array[String]
  ): scala.collection.Map[String, Array[Annotation]] =
    findAnnotations(clazz, clazz.getConstructors.head.getParameterTypes, fields)

  /**
   * NOTE: THIS IS AN INTERNAL FRAMEWORK API AND NOT INTENDED FOR PUBLIC USAGE.
   *
   * Attempts to find annotations per `case class` field and declared methods returning a mapping of field name to list
   * of any found annotations. For Scala case classes, annotations are only visible on the constructor,
   * thus this code needs to know which constructor of the given class to inspect for annotations.
   * If a suitable constructor cannot be located given the passed `parameterTypes` this function
   * will not error but instead skips field. This is intended to
   * not fail if the parameter types represent a static constructor or factory method defined in a
   * different class and instead continues onto the method annotation scanning.
   *
   * @note IMPORTANT: this is only intended for Scala case classes as it only tries to find annotations
   *       on constructors of the given class.
   * @param clazz the `Class` to inspect. This should represent a Scala case class.
   * @param parameterTypes the list of parameter class types in order to locate the appropriate
   *                       case class constructor for use in annotation scanning. If a suitable
   *                       constructor cannot be located, field annotation scanning is effectively
   *                       skipped (since annotations are carried only on constructors in Scala).
   * @param fields the list of case class fields.
   * @return a mapping of field name to list of annotations. Note, this only returns fields which have
   *         annotations.
   *
   * @see [[https://docs.scala-lang.org/tour/case-classes.html Tour of Scala Case Classes]]
   */
  private[twitter] def findAnnotations(
    clazz: Class[_],
    parameterTypes: Seq[Class[_]],
    fields: Array[String]
  ): scala.collection.Map[String, Array[Annotation]] = {
    val clazzAnnotations = scala.collection.mutable.HashMap[String, Array[Annotation]]()
    // for case classes, the annotations are only visible on the constructor.
    // can blow up if we are passed incorrect parameter types or parameter types for a static
    // constructor not defined on this class. we move on if we cannot locate a constructor
    // by the given parameter types.
    val constructor = Try(clazz.getConstructor(parameterTypes: _*))
    constructor match {
      case Return(cons) =>
        val clazzConstructorAnnotations: Array[Array[Annotation]] =
          cons.getParameterAnnotations
        var index = 0
        while (index < fields.length) {
          val field = fields(index)
          val fieldAnnotations = clazzConstructorAnnotations(index)
          if (fieldAnnotations.nonEmpty) clazzAnnotations.put(field, fieldAnnotations)
          index += 1
        }
      case _ => // do nothing
    }

    // find inherited annotations
    findDeclaredMethodAnnotations(
      clazz,
      clazzAnnotations
    )
  }

  private[twitter] def findAnnotations(
    clazz: Class[_],
    fieldAnnotations: scala.collection.Map[String, Array[Annotation]]
  ): scala.collection.Map[String, Array[Annotation]] = {
    val collectorMap = new scala.collection.mutable.HashMap[String, Array[Annotation]]()
    collectorMap ++= fieldAnnotations
    // find inherited annotations
    findDeclaredMethodAnnotations(
      clazz,
      collectorMap
    )
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

  // optimized
  private[this] def findDeclaredMethodAnnotations(
    clazz: Class[_],
    acc: scala.collection.mutable.Map[String, Array[Annotation]]
  ): scala.collection.Map[String, Array[Annotation]] = {

    val methods = clazz.getDeclaredMethods
    var i = 0
    while (i < methods.length) {
      val method = methods(i)
      val methodAnnotations = method.getDeclaredAnnotations
      if (methodAnnotations.nonEmpty) {
        acc.get(method.getName) match {
          case Some(existing) =>
            acc.put(method.getName, mergeAnnotationLists(existing, methodAnnotations))
          case _ =>
            acc.put(method.getName, methodAnnotations)
        }
      }
      i += 1
    }

    val interfaces = clazz.getInterfaces
    var j = 0
    while (j < interfaces.length) {
      val interface = interfaces(j)
      findDeclaredMethodAnnotations(interface, acc)
      j += 1
    }

    acc
  }

  /** Prefer values in A over B */
  private[this] def mergeAnnotationLists(
    a: Array[Annotation],
    b: Array[Annotation]
  ): Array[Annotation] =
    a ++ b.filterNot(bAnnotation => a.exists(_.annotationType() == bAnnotation.annotationType()))
}
