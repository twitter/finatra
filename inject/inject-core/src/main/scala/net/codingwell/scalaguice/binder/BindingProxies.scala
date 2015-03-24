/*
 *  Copyright 2010-2011 Benjamin Lings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.codingwell.scalaguice
package binder

import com.google.inject._
import com.google.inject.binder._
import java.lang.annotation.{Annotation => JAnnotation}
import java.lang.reflect.{Constructor => JConstructor}
import scala.language.postfixOps

/**
 * Proxy for [[com.google.inject.binder.ScopedBindingBuilder]]
 */
trait ScopedBindingBuilderProxy extends ScopedBindingBuilder
                          with Proxy {

  override def self: ScopedBindingBuilder

  def asEagerSingleton = self asEagerSingleton
  def in(scope: Scope) = self in scope
  def in(scopeAnnotation: Class[_ <: JAnnotation]) = self in scopeAnnotation
}

/**
 * Proxy for [[com.google.inject.binder.LinkedBindingBuilder]]
 */
trait LinkedBindingBuilderProxy[T] extends LinkedBindingBuilder[T]
                          with ScopedBindingBuilderProxy {

  override def self: LinkedBindingBuilder[T]

  override def to(implementation: Class[_ <: T]) = self to implementation
  override def to(implementation: TypeLiteral[_ <: T]) = self to implementation
  override def to(targetKey: Key[_ <: T]) = self to targetKey
  override def toConstructor[S <: T](constructor:JConstructor[S]) = self toConstructor(constructor)
  override def toConstructor[S <: T](constructor:JConstructor[S], literal:TypeLiteral[_ <: S]) = self toConstructor(constructor,literal)
  override def toInstance(instance: T) = self toInstance instance
  override def toProvider(provider: Provider[_ <: T]) = self toProvider provider
  override def toProvider(provider: Class[_ <: javax.inject.Provider[_ <: T]]) = self toProvider provider
  override def toProvider(provider: TypeLiteral[_ <: javax.inject.Provider[_ <: T]]) = self toProvider provider
  override def toProvider(providerKey: Key[_ <: javax.inject.Provider[_ <: T]]) = self toProvider providerKey
}

/**
 * Proxy for [[com.google.inject.binder.AnnotatedBindingBuilder]]
 */
trait AnnotatedBindingBuilderProxy[T] extends AnnotatedBindingBuilder[T]
                           with LinkedBindingBuilderProxy[T] {

  override def self: AnnotatedBindingBuilder[T]

  def annotatedWith(annotation: JAnnotation) = self annotatedWith annotation
  def annotatedWith(annotationType: Class[_ <: JAnnotation]) = self annotatedWith annotationType
}

/**
 * Proxy for [[com.google.inject.binder.AnnotatedElementBuilder]]
 */
trait AnnotatedElementBuilderProxy[T] extends AnnotatedElementBuilder with Proxy {
  override def self: AnnotatedElementBuilder

  def annotatedWith(annotation: JAnnotation) = self annotatedWith annotation
  def annotatedWith(annotationType: Class[_ <: JAnnotation]) = self annotatedWith annotationType
}