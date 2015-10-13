package com.twitter.inject

import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.matcher.Matchers
import com.google.inject.spi.TypeConverter
import com.google.inject.{Module, _}
import java.lang.annotation.Annotation
import net.codingwell.scalaguice.ScalaModule.ScalaAnnotatedBindingBuilder
import net.codingwell.scalaguice.{ScalaMultibinder, typeLiteral}


abstract class TwitterModule
  extends AbstractModule
  with TwitterBaseModule
  with Logging {

  private def binderAccess = super.binder

  /* Overrides */

  // Provide default configure method so Modules using only @Provider don't need an empty configure method
  override protected def configure() {}

  override protected def install(module: Module) {
    module match {
      case twitterModule: TwitterModule =>
        throw new Exception("Install not supported for TwitterModules. Please use 'override val modules = Seq(module1, module2, ...)'")
      case _ =>
        super.install(module)
    }
  }

  /* Protected */

  protected def getProvider[T: Manifest]: Provider[T] = {
    getProvider(createKey[T])
  }

  protected def bindAssistedFactory[T: Manifest] {
    super.install(
      new FactoryModuleBuilder().build(manifest[T].runtimeClass))
  }

  protected def addTypeConvertor[T: Manifest](converter: TypeConverter) {
    convertToTypes(
      Matchers.only(typeLiteral[T]),
      converter)
  }

  /* Protected "Bind" methods
     We copy the stack-trace preserving hacks found in:
     https://github.com/codingwell/scala-guice/blob/v3.0.2/src/main/scala/net/codingwell/scalaguice/ScalaModule.scala */

  protected def bindSingleton[T: Manifest]: ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder = createBuilder()
      override val self = builder
    }
  }

  protected def bindSingleton[T: Manifest](annot: Annotation): ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder = createBuilder(annotationOpt = Some(annot))
      override val self = builder
    }
  }

  protected def bind[T: Manifest](annot: Annotation): ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder = createBuilder(annotationOpt = Some(annot), singleton = false)
      override val self = builder
    }
  }

  protected def bind[T: Manifest]: ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder = createBuilder(singleton = false)
      override val self = builder
    }
  }

  protected def createMultiBinder[MultiBindType: Manifest] = {
    ScalaMultibinder.newSetBinder[MultiBindType](binder.withSource((new Throwable).getStackTrace()(1)))
  }

  /* Copying stacktrace hacks found in scalaguice's ScalaModule.scala */
  private def createBuilder[T: Manifest](annotationOpt: Option[Annotation] = None, singleton: Boolean = true) = {
    val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))
    val builder = mybinder bind typeLiteral[T]

    /* Set as singleton */
    if (singleton) {
      builder.in(Scopes.SINGLETON)
    }

    /* Set annotation if specified */
    for (annot <- annotationOpt) {
      builder.annotatedWith(annot)
    }

    builder
  }
}