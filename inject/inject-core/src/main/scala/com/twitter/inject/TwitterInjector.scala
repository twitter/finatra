package com.twitter.inject

import com.google.inject.{Key, Injector => UnderlyingInjector}
import com.google.inject.name.Names
import java.lang.annotation.Annotation
import net.codingwell.scalaguice.KeyExtensions._
import net.codingwell.scalaguice.typeLiteral
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * An [[Injector]] implementation based of Guice's `Injector`.
 */
private final class TwitterInjector(val underlying: UnderlyingInjector) extends Injector {

  def instance[T: TypeTag]: T =
    underlying.getInstance(typeLiteral[T].toKey)

  def instance[T: TypeTag, Ann <: Annotation: ClassTag]: T =
    underlying.getInstance(typeLiteral[T].annotatedWith[Ann])

  def instance[T: TypeTag](annotation: Annotation): T =
    underlying.getInstance(typeLiteral[T].annotatedWith(annotation))

  def instance[T: TypeTag](annotationClazz: Class[_ <: Annotation]): T =
    underlying.getInstance(typeLiteral[T].annotatedWith(annotationClazz))

  def instance[T: TypeTag](name: String): T =
    underlying.getInstance(typeLiteral[T].annotatedWith(Names.named(name)))

  def instance[T](clazz: Class[T]): T = underlying.getInstance(clazz)

  def instance[T](clazz: Class[T], annotation: Annotation): T =
    underlying.getInstance(Key.get(clazz, annotation))

  def instance[T, Ann <: Annotation](clazz: Class[T], annotationClazz: Class[Ann]): T =
    underlying.getInstance(Key.get(clazz, annotationClazz))

  def instance[T](key: Key[T]): T = underlying.getInstance(key)
}
