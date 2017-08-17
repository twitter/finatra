/*
   Copyright 2010 RightTime, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.twitter.finatra.http.internal.marshalling.mustache

import com.github.mustachejava.Iteration
import com.github.mustachejava.reflect.ReflectionObjectHandler
import com.twitter.util.{Await, Future}
import java.io.Writer
import java.lang.reflect.{Field, Method}
import java.util.concurrent.Callable
import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import scala.runtime.BoxedUnit

/*
 * We need to copy a mustache.java class here since:
 * - 0.8.x doesn't support 2.11
 * - 0.9.x is Java 8 only
 *
 * Copied from https://github.com/spullara/mustache.java/blob/master/scala-extensions/scala-extensions-2.10/src/main/scala/com/twitter/mustache/ScalaObjectHandler.scala
 * and combined with TwitterObjectHandler for Future support.
 *
 * TODO: Remove once we can use the scala-extensions-2.11
 */
/**
 * Plain old scala handler that doesn't depend on Twitter libraries.
 */
class ScalaObjectHandler extends ReflectionObjectHandler {

  // Allow any method or field
  override def checkMethod(member: Method) {}

  override def checkField(member: Field) {}

  override def coerce(value: AnyRef) = {
    value match {
      case f: Future[_] =>
        new Callable[Any]() {
          def call() = {
            val value = Await.result(f).asInstanceOf[Object]
            coerce(value)
          }
        }
      case m: collection.Map[_, _] => mapAsJavaMap(m)
      case u: BoxedUnit => null
      case Some(some: AnyRef) => coerce(some)
      case None => null
      case _ => value
    }
  }

  override def iterate(iteration: Iteration, writer: Writer, value: AnyRef, scopes: Array[AnyRef]) = {
    value match {
      case TraversableAnyRef(t) => {
        var newWriter = writer
        t foreach { next =>
          newWriter = iteration.next(newWriter, coerce(next), scopes)
        }
        newWriter
      }
      case n: Number =>
        if (n.intValue() == 0) writer else iteration.next(writer, coerce(value), scopes)
      case _ => super.iterate(iteration, writer, value, scopes)
    }
  }

  override def falsey(iteration: Iteration, writer: Writer, value: AnyRef, scopes: Array[AnyRef]) = {
    value match {
      case TraversableAnyRef(t) => {
        if (t.isEmpty) {
          iteration.next(writer, value, scopes)
        } else {
          writer
        }
      }
      case n: Number =>
        if (n.intValue() == 0) iteration.next(writer, coerce(value), scopes) else writer
      case _ => super.falsey(iteration, writer, value, scopes)
    }
  }

  val TraversableAnyRef = new Def[Traversable[AnyRef]]

  class Def[C: ClassTag] {
    def unapply[X: ClassTag](x: X): Option[C] = {
      x match {
        case c: C => Some(c)
        case _ => None
      }
    }
  }

}
