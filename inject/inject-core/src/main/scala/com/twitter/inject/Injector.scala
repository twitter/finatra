package com.twitter.inject

import com.google.inject.name.Names
import com.google.inject.{Injector => UnderlyingInjector, Key}
import java.lang.annotation.Annotation
import net.codingwell.scalaguice.KeyExtensions._
import net.codingwell.scalaguice._

case class Injector(underlying: UnderlyingInjector) {

  /**
   * Returns the appropriate instance for the given key constructed from the
   * passed type [[T]].
   *
   * When feasible, avoid using this method in favor of having the Injector
   * inject your dependencies ahead of time via annotating your constructor
   * with `@Inject`.
   *
   * @tparam T type of the bound instance to return from the object graph.
   *
   * @return bound instance of type [[T]].
   */
  def instance[T: Manifest]: T = underlying.getInstance(typeLiteral[T].toKey)

  /**
   * Returns the appropriate instance for the given key constructed from the
   * passed type [[T]] and given [[java.lang.annotation.Annotation]] type [[Ann]].
   *
   * When feasible, avoid using this method in favor of having the Injector
   * inject your dependencies ahead of time via annotating your constructor
   * with `@Inject`.
   *
   * @tparam T type of the bound instance to return from the object graph.
   * @tparam Ann type of the annotation used to disambiguate the bound type [[T]].
   *
   * @return bound instance of type [[T]] annotated with annotation type [[Ann]].
   */
  def instance[T: Manifest, Ann <: Annotation: Manifest]: T = {
    val annotationType = manifest[Ann].runtimeClass.asInstanceOf[Class[Ann]]
    val key = Key.get(typeLiteral[T], annotationType)
    underlying.getInstance(key)
  }

  /**
   * Returns the appropriate instance for the given key constructed from the
   * passed type [[T]] and given [[java.lang.annotation.Annotation]] annotation.
   *
   * When feasible, avoid using this method in favor of having the Injector
   * inject your dependencies ahead of time via annotating your constructor
   * with `@Inject`.
   *
   * @param annotation [[java.lang.annotation.Annotation]] instance used to
   *                    disambiguate the bound type [[T]].
   * @tparam T type of the bound instance to return from the object graph.
   *
   * @return bound instance of type [[T]] annotated with annotation.
   */
  def instance[T: Manifest](annotation: Annotation): T = {
    val key = Key.get(typeLiteral[T], annotation)
    underlying.getInstance(key)
  }

  /**
   * Returns the appropriate instance for the given key constructed from the
   * passed type [[T]] and given [[java.lang.annotation.Annotation]] class.
   *
   * When feasible, avoid using this method in favor of having the Injector
   * inject your dependencies ahead of time via annotating your constructor
   * with `@Inject`.
   *
   * @param annotationClazz class of [[java.lang.annotation.Annotation]] used
   *                        to disambiguate the bound type [[T]].
   * @tparam T type of the bound instance to return from the object graph.
   * @return bound instance of type [[T]] annotated with annotation class.
   */
  def instance[T: Manifest](annotationClazz: Class[_ <: Annotation]): T = {
    val key = Key.get(typeLiteral[T], annotationClazz)
    underlying.getInstance(key)
  }

  /**
   * Returns the appropriate instance for the given key constructed from the
   * passed type [[T]] and given String name which is interpreted to be the
   * value of a @Named annotation.
   *
   * When feasible, avoid using this method in favor of having the Injector
   * inject your dependencies ahead of time via annotating your constructor
   * with `@Inject`.
   *
   * @param name String value of `@Named` annotation.
   * @tparam T type of the bound instance to return from the object graph.
   *
   * @return bound instance of type [[T]] annotated with `@Named(name)`.
   *
   * @see [[https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/name/Named.html com.google.inject.name.Named]]
   */
  @deprecated(
    "Users should prefer injector.instance[T](java.lang.annotation.Annotation",
    "2017-09-25")
  def instance[T: Manifest](name: String): T = {
    val namedAnnotation = Names.named(name)
    val key = Key.get(typeLiteral[T], namedAnnotation)
    underlying.getInstance(key)
  }

  /**
   * Returns the appropriate instance for the given injection type.
   *
   * When feasible, avoid using this method, in favor of having the Injector
   * inject your dependencies ahead of time via annotating your constructor
   * with `@Inject`.
   *
   * @param clazz the class of type [[T]] of the bound instance to return
   *              from the object graph.
   * @tparam T type of the bound instance to return from the object graph.
   *
   * @return bound instance of type [[T]].
   */
  def instance[T](clazz: Class[T]): T = underlying.getInstance(clazz)

  /**
   * Returns the appropriate instance for the given key constructed from the
   * passed class and given [[java.lang.annotation.Annotation]] annotation.
   *
   * When feasible, avoid using this method, in favor of having the Injector
   * inject your dependencies ahead of time via annotating your constructor
   * with `@Inject`.
   *
   * @param clazz the class of type [[T]] of the bound instance to return from
   *              the object graph.
   * @param annotation [[java.lang.annotation.Annotation]] instance used to
   *                  disambiguate the bound type [[T]].
   * @tparam T type of the bound instance to return from the object graph.
   *
   * @return bound instance of type [[T]].
   */
  def instance[T](clazz: Class[T], annotation: Annotation): T = {
    val key = Key.get(clazz, annotation)
    underlying.getInstance(key)
  }

  /**
   * Returns the appropriate instance for the given key constructed from the
   * passed class and given [[java.lang.annotation.Annotation]] class.
   *
   * @param clazz the class of type [[T]] of the bound instance to return from
   *              the object graph.
   * @param annotationClazz [[java.lang.annotation.Annotation]] class used to
   *                        disambiguate the bound type [[T]].
   * @tparam T type of the bound instance to return from the object graph.
   * @tparam Ann type of the annotation class used to disambiguate the bound type [[T]].
   *
   * @return bound instance of type [[T]].
   */
  def instance[T, Ann <: Annotation](clazz: Class[T], annotationClazz: Class[Ann]): T = {
    val key = Key.get(clazz, annotationClazz)
    underlying.getInstance(key)
  }

  /**
   * Returns the appropriate instance for the given injection key.
   *
   * When feasible, avoid using this method in favor of having the Injector
   * inject your dependencies ahead of time via annotating your constructor
   * with `@Inject`.
   *
   * @param key [[com.google.inject.Key]] binding key of the bound instance to return
   *              from the object graph.
   * @tparam T type of the bound instance to return from the object graph.
   *
   * @return bound instance of type [[T]] represented by [[com.google.inject.Key]] key.
   *
   * @see [[https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/Key.html com.google.inject.Key]]
   */
  def instance[T](key: Key[T]): T = underlying.getInstance(key)
}
