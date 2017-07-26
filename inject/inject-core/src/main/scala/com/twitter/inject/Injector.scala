package com.twitter.inject

import com.google.inject.name.Names
import com.google.inject.{Injector => UnderlyingInjector, Key}
import java.lang.annotation.Annotation
import net.codingwell.scalaguice.KeyExtensions._
import net.codingwell.scalaguice._

case class Injector(underlying: UnderlyingInjector) {

  def instance[T: Manifest]: T = underlying.getInstance(typeLiteral[T].toKey)

  def instance[T: Manifest, Ann <: Annotation: Manifest]: T = {
    val annotationType = manifest[Ann].runtimeClass.asInstanceOf[Class[Ann]]
    val key = Key.get(typeLiteral[T], annotationType)
    underlying.getInstance(key)
  }

  def instance[T: Manifest](name: String): T = {
    val namedAnnotation = Names.named(name)
    val key = Key.get(typeLiteral[T], namedAnnotation)
    underlying.getInstance(key)
  }

  def instance[T](clazz: Class[T]): T = underlying.getInstance(clazz)

  def instance[T](key: Key[T]): T = underlying.getInstance(key)
}
