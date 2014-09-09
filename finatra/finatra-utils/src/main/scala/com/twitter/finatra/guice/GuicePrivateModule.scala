package com.twitter.finatra.guice

import com.google.inject._
import com.google.inject.name.Names
import com.twitter.finatra.annotations.FlagImpl
import java.lang.annotation.{Annotation => JAnnotation}
import net.codingwell.scalaguice._

// Copying hacks in scalaguice.PrivateScalaModule.scala
// NOTE: Intellij presentation compiler incorrectly shows errors in ScalaAnnotatedBindingBuilder
abstract class GuicePrivateModule
  extends PrivateModule
  with ScalaPrivateModule {

  import net.codingwell.scalaguice.ScalaModule._

  private def binderAccess = super.binder


  def bindAndExpose[Trait: Manifest, Annot <: JAnnotation : Manifest, Impl <: Trait : Manifest] = {
    new ScalaAnnotatedBindingBuilder[Trait] {
      val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))

      val builder = mybinder bind typeLiteral[Trait]
      builder.annotatedWith(annotation[Annot])
      builder.to(typeLiteral[Impl])

      val self = builder
    }
    new ScalaAnnotatedElementBuilder[Trait] {
      val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))

      val builder = mybinder expose typeLiteral[Trait]
      builder.annotatedWith(annotation[Annot])

      val self = builder
    }
  }

  def bindString(name: String, value: String) = new ScalaAnnotatedBindingBuilder[String] {
    val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))
    val builder = mybinder bind typeLiteral[String]
    builder.annotatedWith(Names.named(name))
    builder.toInstance(value)
    val self = builder
  }

  def bindFlag(name: String, value: String) = new ScalaAnnotatedBindingBuilder[String] {
    val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))
    val builder = mybinder bind typeLiteral[String]
    builder.annotatedWith(new FlagImpl(name))
    builder.toInstance(value)
    val self = builder
  }

  def bindToAnnotation[T: Manifest, A <: JAnnotation : Manifest] = new ScalaAnnotatedBindingBuilder[T] {
    val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))
    val builder = mybinder bind typeLiteral[T]
    val key = Key.get(typeLiteral[T], annotation[A])
    builder.to(key)
    val self = builder
  }
}