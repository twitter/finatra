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

import com.google.inject._

object KeyExtensions {

    import java.lang.annotation.{Annotation => JAnnotation}

    implicit class RichTypeLiteral[T](val t: TypeLiteral[T]) extends AnyVal {
        def toKey: Key[T] = Key.get(t)
        def annotatedWith(annotation: JAnnotation): Key[T] = Key.get(t, annotation)
        def annotatedWith[TAnn <: JAnnotation : Manifest]:Key[T] =
            Key.get(t, annotation[TAnn])
    }
}