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
// Modified to remove 2.10 deprecations
package net.codingwell

import com.google.inject.util.Types
import scala.reflect.ClassTag

package object scalaguice {

    import com.google.inject._
    import java.lang.reflect.Type
  
    private def isArray[T](implicit m:Manifest[T]) = m.runtimeClass.isArray

    private[scalaguice] def typeOf[T](implicit m: Manifest[T]): Type = {
        def toWrapper(c:Type) = c match {
            case java.lang.Byte.TYPE => classOf[java.lang.Byte]
            case java.lang.Short.TYPE => classOf[java.lang.Short]
            case java.lang.Character.TYPE => classOf[java.lang.Character]
            case java.lang.Integer.TYPE => classOf[java.lang.Integer]
            case java.lang.Long.TYPE => classOf[java.lang.Long]
            case java.lang.Float.TYPE => classOf[java.lang.Float]
            case java.lang.Double.TYPE => classOf[java.lang.Double]
            case java.lang.Boolean.TYPE => classOf[java.lang.Boolean]
            case java.lang.Void.TYPE => classOf[java.lang.Void]
            case cls => cls
        }

        if( isArray[T] ) return m.runtimeClass

        import com.google.inject.util.Types
        m.typeArguments match {
            case Nil => toWrapper(m.runtimeClass)
            case args => m.runtimeClass match {
                case c:Class[_] if c.getEnclosingClass == null => Types.newParameterizedType(c, args.map(typeOf(_)):_*)
                case c:Class[_] => Types.newParameterizedTypeWithOwner(c.getEnclosingClass, c, args.map(typeOf(_)):_*)
            }
        }
    }

    /**
     * Create a [[com.google.inject.TypeLiteral]] from a [[scala.Manifest]].
     * Subtypes of [[scala.AnyVal]] will be converted to their corresponding
     * Java wrapper classes.
     */
    def typeLiteral[T : Manifest]: TypeLiteral[T] = {
        TypeLiteral.get(typeOf[T]).asInstanceOf[TypeLiteral[T]]
    }

    import java.lang.annotation.{Annotation => JAnnotation}

    type AnnotationClass[T <: JAnnotation] = Class[T]

    /**
     * Get the class for a Java Annotation using a [[scala.Manifest]].
     */
    def annotation[T <: JAnnotation : Manifest]: AnnotationClass[T] = {
        manifest[T].runtimeClass.asInstanceOf[AnnotationClass[T]]
    }
}