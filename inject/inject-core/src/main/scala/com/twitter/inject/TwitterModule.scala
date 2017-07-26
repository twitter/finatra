package com.twitter.inject

import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.matcher.Matchers
import com.google.inject.spi.TypeConverter
import com.google.inject.{Module, _}
import java.lang.annotation.Annotation
import net.codingwell.scalaguice.ScalaModule.ScalaAnnotatedBindingBuilder
import net.codingwell.scalaguice.{ScalaMultibinder, typeLiteral}

abstract class TwitterModule extends AbstractModule with TwitterBaseModule with Logging {

  private def binderAccess = super.binder

  /* Overrides */

  // Provide default configure method so Modules using only @Provider don't need an empty configure method
  override protected def configure(): Unit = {}

  override protected def install(module: Module): Unit = {
    module match {
      case twitterModule: TwitterModule =>
        throw new Exception(
          "Install not supported for TwitterModules. Please use 'override val modules = Seq(module1, module2, ...)'"
        )
      case _ =>
        super.install(module)
    }
  }

  /* Protected */

  protected def getProvider[T: Manifest]: Provider[T] = {
    getProvider(createKey[T])
  }

  protected def bindAssistedFactory[T: Manifest](): Unit = {
    super.install(new FactoryModuleBuilder().build(manifest[T].runtimeClass))
  }

  protected def addTypeConverter[T: Manifest](converter: TypeConverter): Unit = {
    convertToTypes(Matchers.only(typeLiteral[T]), converter)
  }

  /* Protected "Bind" methods
     We copy the stack-trace preserving hacks found in:
     https://github.com/codingwell/scala-guice/blob/v3.0.2/src/main/scala/net/codingwell/scalaguice/ScalaModule.scala */

  protected def bindSingleton[T: Manifest]: ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder: AnnotatedBindingBuilder[T] = createBuilder[T]()
      override val self = builder
    }
  }

  protected def bindSingleton[T: Manifest](
    annotation: Annotation
  ): ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder: AnnotatedBindingBuilder[T] = createBuilder[T](annotationOpt = Some(annotation))
      override val self = builder
    }
  }

  protected def bindSingleton[T: Manifest, A <: Annotation: Manifest]
    : ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder: AnnotatedBindingBuilder[T] = createBuilderWithAnnotation[T, A]()
      override val self = builder
    }
  }

  protected def bind[T: Manifest]: ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder: AnnotatedBindingBuilder[T] = createBuilder[T](singleton = false)
      override val self = builder
    }
  }

  protected def bind[T: Manifest](annotation: Annotation): ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder: AnnotatedBindingBuilder[T] =
        createBuilder[T](annotationOpt = Some(annotation), singleton = false)
      override val self = builder
    }
  }

  protected def bind[T: Manifest, A <: Annotation: Manifest]: ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder: AnnotatedBindingBuilder[T] = createBuilderWithAnnotation[T, A](singleton = false)
      override val self = builder
    }
  }

  protected def createMultiBinder[MultiBindType: Manifest]: ScalaMultibinder[MultiBindType] = {
    ScalaMultibinder.newSetBinder[MultiBindType](
      binder.withSource((new Throwable).getStackTrace()(1))
    )
  }

  /* Private */

  /* Copying stacktrace hacks found in scalaguice's ScalaModule.scala */
  private def createBuilder[T: Manifest](
    annotationOpt: Option[Annotation] = None,
    singleton: Boolean = true
  ): AnnotatedBindingBuilder[T] = {
    val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))
    val builder = mybinder bind typeLiteral[T]

    /* Set as singleton */
    if (singleton) {
      builder.in(Scopes.SINGLETON)
    }

    /* Set annotation if specified */
    for (annotation <- annotationOpt) {
      builder.annotatedWith(annotation)
    }

    builder
  }

  /* Copying stacktrace hacks found in scalaguice's ScalaModule.scala */
  private def createBuilderWithAnnotation[T: Manifest, A <: Annotation: Manifest](
    singleton: Boolean = true
  ): AnnotatedBindingBuilder[T] = {
    val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))
    val builder = mybinder bind typeLiteral[T]

    /* Set as singleton */
    if (singleton) {
      builder.in(Scopes.SINGLETON)
    }

    /* Set annotation */
    builder.annotatedWith(manifest[A].runtimeClass.asInstanceOf[Class[_ <: Annotation]])

    builder
  }
}
