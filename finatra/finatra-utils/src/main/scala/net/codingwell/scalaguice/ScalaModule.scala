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
import binder._

/**
 * Allows binding via type parameters. Mix into <code>AbstractModule</code>
 *  (or subclass) to allow using a type parameter instead of
 * <code>classOf[Foo]</code> or <code>new TypeLiteral[Bar[Foo]] {}</code>.
 *
 * For example, instead of
 * {{{
 * class MyModule extends AbstractModule {
 *   def configure {
 *     bind(classOf[Service]).to(classOf[ServiceImpl]).in(classOf[Singleton])
 *     bind(classOf[CreditCardPaymentService])
 *     bind(new TypeLiteral[Bar[Foo]]{}).to(classOf[FooBarImpl])
 *     bind(classOf[PaymentService]).to(classOf[CreditCardPaymentService])
 *   }
 * }
 * }}}
 * use
 * {{{
 * class MyModule extends AbstractModule with ScalaModule {
 *   def configure {
 *     bind[Service].to[ServiceImpl].in[Singleton]
 *     bind[CreditCardPaymentService]
 *     bind[Bar[Foo]].to[FooBarImpl]
 *     bind[PaymentService].to[CreditCardPaymentService]
 *   }
 * }
 * }}}
 *
 * '''Note''' This syntax allows binding to and from generic types.
 * It doesn't currently allow bindings between wildcard types because the
 * manifests for wildcard types don't provide access to type bounds.
 */
trait ScalaModule extends AbstractModule {
  // should be:
  // this: AbstractModule =>
  // see http://lampsvn.epfl.ch/trac/scala/ticket/3564

  import ScalaModule._

  private def binderAccess = super.binder // shouldn't need super

  def bind[T: Manifest] = new ScalaAnnotatedBindingBuilder[T] {
     //Hack, no easy way to exclude the bind method that gets added to classes inheriting ScalaModule
     //So we experamentally figured out how many calls up is the source, so we use that
     //Commit 52c2e92f8f6131e4a9ea473f58be3e32cd172ce6 has better class exclusion
     val mybinder = binderAccess.withSource( (new Throwable).getStackTrace()(3) )
     val self = mybinder bind typeLiteral[T]
  }

}

trait ScalaPrivateModule extends PrivateModule {
  // should be:
  // this: PrivateModule =>
  // see http://lampsvn.epfl.ch/trac/scala/ticket/3564

  import ScalaModule._

  private def binderAccess = super.binder // shouldn't need super

  def bind[T: Manifest] = new ScalaAnnotatedBindingBuilder[T] {
     //Hack, no easy way to exclude the bind method that gets added to classes inheriting ScalaModule
     //So we experamentally figured out how many calls up is the source, so we use that
     //Commit 52c2e92f8f6131e4a9ea473f58be3e32cd172ce6 has better class exclusion
     val mybinder = binderAccess.withSource( (new Throwable).getStackTrace()(3) )
     val self = mybinder bind typeLiteral[T]
  }

  def expose[T: Manifest] = new ScalaAnnotatedElementBuilder[T] {
     //Hack, no easy way to exclude the bind method that gets added to classes inheriting ScalaModule
     //So we experamentally figured out how many calls up is the source, so we use that
     //Commit 52c2e92f8f6131e4a9ea473f58be3e32cd172ce6 has better class exclusion
     val mybinder = binderAccess.withSource( (new Throwable).getStackTrace()(3) )
     val self = mybinder expose typeLiteral[T]
  }

}

object ScalaModule {
  import java.lang.annotation.{Annotation => JAnnotation}

  trait ScalaScopedBindingBuilder extends ScopedBindingBuilderProxy {
    def in[TAnn <: JAnnotation : ClassManifest] = self in annotation[TAnn]
  }

  trait ScalaLinkedBindingBuilder[T] extends ScalaScopedBindingBuilder
    with LinkedBindingBuilderProxy[T] { outer =>
    def to[TImpl <: T : Manifest] = new ScalaScopedBindingBuilder {
      val self = outer.self to typeLiteral[TImpl]
    }
    def toProvider[TProvider <: Provider[_ <: T] : Manifest] = new ScalaScopedBindingBuilder {
      val self = outer.self toProvider typeLiteral[TProvider]
    }
  }

  trait ScalaAnnotatedBindingBuilder[T] extends ScalaLinkedBindingBuilder[T]
    with AnnotatedBindingBuilderProxy[T] { outer =>
    def annotatedWith[TAnn <: JAnnotation : ClassManifest] = new ScalaLinkedBindingBuilder[T] {
      val self = outer.self annotatedWith annotation[TAnn]
    }
  }

  trait ScalaAnnotatedElementBuilder[T] extends AnnotatedElementBuilderProxy[T] {
    def annotatedWith[TAnn <: JAnnotation : ClassManifest] = self annotatedWith annotation[TAnn]
  }

}