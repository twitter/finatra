/*
 *  Copyright 2012 Benjamin Lings
 *  Author: Thomas Suckow
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

import com.google.inject._
import com.google.inject.multibindings._

import java.lang.annotation.Annotation
import java.util.{Set => JSet}

import scala.collection.{ immutable => im }

/**
 * Analog to the Guice Multibinder
 *
 * Use {@code newSetBinder} to create a multibinder instance
*/
object ScalaMultibinder {
  /**
   * Returns a new multibinder that collects instances of {@code settype} in a [[scala.collection.immutable.Set]] that is
   * itself bound with no binding annotation.
   */
  def newSetBinder[T : Manifest]( binder:Binder, settype:TypeLiteral[T] ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val result = Multibinder.newSetBinder( mybinder, settype )
    binder.bind( Key.get( typeLiteral[im.Set[T]] ) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]] ) ) )
    new ScalaMultibinder( binder, result )
  }

  /**
   * Returns a new multibinder that collects instances of {@code settype} in a [[scala.collection.immutable.Set]] that is
   * itself bound with no binding annotation.
   */
  def newSetBinder[T : Manifest]( binder:Binder, settype:Class[T] ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val result = Multibinder.newSetBinder( mybinder, settype )
    binder.bind( Key.get( typeLiteral[im.Set[T]] ) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]] ) ) )
    new ScalaMultibinder( binder, result )
  }

  /**
   * Returns a new multibinder that collects instances of {@code settype} in a [[scala.collection.immutable.Set]] that is
   * itself bound with a binding annotation.
   */
  def newSetBinder[T : Manifest]( binder:Binder, settype:TypeLiteral[T], annotation:Annotation ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val result = Multibinder.newSetBinder( mybinder, settype, annotation )
    binder.bind( Key.get( typeLiteral[im.Set[T]], annotation) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]], annotation ) ) )
    new ScalaMultibinder( binder, result )
  }

  /**
   * Returns a new multibinder that collects instances of {@code settype} in a [[scala.collection.immutable.Set]] that is
   * itself bound with a binding annotation.
   */
  def newSetBinder[T : Manifest]( binder:Binder, settype:Class[T], annotation:Annotation ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val result = Multibinder.newSetBinder( mybinder, settype, annotation )
    binder.bind( Key.get( typeLiteral[im.Set[T]], annotation) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]], annotation ) ) )
    new ScalaMultibinder( binder, result )
  }

  /**
   * Returns a new multibinder that collects instances of {@code settype} in a [[scala.collection.immutable.Set]] that is
   * itself bound with a binding annotation.
   */
  def newSetBinder[T : Manifest]( binder:Binder, settype:TypeLiteral[T], annotation:Class[_ <: Annotation] ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val result = Multibinder.newSetBinder( mybinder, settype, annotation )
    binder.bind( Key.get( typeLiteral[im.Set[T]], annotation) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]], annotation ) ) )
    new ScalaMultibinder( binder, result )
  }

  /**
   * Returns a new multibinder that collects instances of {@code settype} in a [[scala.collection.immutable.Set]] that is
   * itself bound with a binding annotation.
   */
  def newSetBinder[T : Manifest]( binder:Binder, settype:Class[T], annotation:Class[_ <: Annotation] ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val result = Multibinder.newSetBinder( mybinder, settype, annotation )
    binder.bind( Key.get( typeLiteral[im.Set[T]], annotation) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]], annotation ) ) )
    new ScalaMultibinder( binder, result )
  }

  /**
   * Returns a new multibinder that collects instances of type {@code T} in a [[scala.collection.immutable.Set]] that is
   * itself bound with no binding annotation.
   */
  def newSetBinder[T : Manifest]( binder:Binder ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val result = Multibinder.newSetBinder( mybinder, typeLiteral[T] )
    binder.bind( Key.get( typeLiteral[im.Set[T]] ) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]] ) ) )
    new ScalaMultibinder( binder, result )
  }

  /**
   * Returns a new multibinder that collects instances of type {@code T} in a [[scala.collection.immutable.Set]] that is
   * itself bound with a binding annotation.
   */
  def newSetBinder[T : Manifest]( binder:Binder, annotation:Annotation ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val result = Multibinder.newSetBinder( mybinder, typeLiteral[T], annotation )
    binder.bind( Key.get( typeLiteral[im.Set[T]], annotation) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]], annotation ) ) )
    new ScalaMultibinder( binder, result )
  }

  /**
   * Returns a new multibinder that collects instances of type {@code T} in a [[scala.collection.immutable.Set]] that is
   * itself bound with a binding annotation {@code Ann}.
   */
  def newSetBinder[T : Manifest, Ann <: Annotation : Manifest]( binder:Binder ) = {
    val mybinder = binder.skipSources( classOf[ScalaMultibinder[T]] )
    val annotation = manifest[Ann].runtimeClass.asInstanceOf[Class[Ann]]
    val result = Multibinder.newSetBinder( mybinder, typeLiteral[T], annotation )
    binder.bind( Key.get( typeLiteral[im.Set[T]], annotation) ).toProvider( new SetProvider[T]( Key.get( typeLiteral[JSet[T]], annotation ) ) )
    new ScalaMultibinder( binder, result )
  }
}

/**
 * Analog to the Guice Multibinder
 */
class ScalaMultibinder[T : Manifest]( binder:Binder, multibinder:Multibinder[T] ) {
  def addBinding() = {
    new ScalaModule.ScalaLinkedBindingBuilder[T] {
      val self = multibinder.addBinding
    }
  }

  def get() = {
    multibinder
  }

  def permitDuplicates():ScalaMultibinder[T] = {
    multibinder.permitDuplicates
    this
  }

}
