package com.twitter.inject

import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.matcher.Matchers
import com.google.inject.spi.TypeConverter
import com.google.inject.{Module, _}
import com.twitter.app.{Flag, FlagFactory, Flaggable}
import java.lang.annotation.Annotation
import net.codingwell.scalaguice.ScalaModule.ScalaAnnotatedBindingBuilder
import net.codingwell.scalaguice.{ScalaMultibinder, annotation, typeLiteral}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


abstract class TwitterModule
  extends AbstractModule
  with Logging {

  private def binderAccess = super.binder

  /* Mutable State */
  private val postStartupFunctions = mutable.Buffer[Injector => Unit]()
  private val shutdownFunctions = mutable.Buffer[() => Unit]()
  protected[inject] val flags = ArrayBuffer[Flag[_]]()

  /* Protected */

  /** Create a flag and add it to the modules flags list */
  protected def flag[T: Flaggable](name: String, default: T, help: String): Flag[T] = {
    val flag = FlagFactory.create(name, default, help)
    flags += flag
    flag
  }

  protected def flag[T: Flaggable : Manifest](name: String, help: String): Flag[T] = {
    val flag = FlagFactory.create[T](name, help)
    flags += flag
    flag
  }

  /** Additional modules to be composed into this module
    * NOTE: This Seq of modules is used instead of the standard Guice 'install' method */
  protected[inject] def modules: Seq[Module] = Seq()

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

  protected def createKey[T: Manifest] = {
    Key.get(typeLiteral[T])
  }

  /*
   * Protected Lifecycle
   * TODO: Eliminate following lifecycle methods by more generally supporting @PostConstruct, @PreDestroy, and @Warmup (see Onami-Lifecycle or Governator)
   */

  /**
   * Invoke `singleton func` after Guice injector is started
   * NOTE: This method should only be called from a @Singleton 'provides' method to avoid registering
   * multiple startup hooks every time an object is created.
   */
  protected def singletonStartup(func: Injector => Unit) {
    postStartupFunctions += func
  }

  /**
   * Invoke 'singleton func' as JVM shuts down.
   * NOTE: This method should only be called from a @Singleton 'provides' method to avoid registering
   * multiple shutdown hooks every time an object is created.
   */
  protected def singletonShutdown(func: => Unit) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() = func
    })
  }

  /* Overrides */

  // Provide default configure method so Module's using only @Provider don't need an empty configure method
  override protected def configure() {}

  override protected def install(module: Module) {
    throw new Exception("Install not supported. Please use 'override val modules = Seq(module1, module2, ...)'")
  }

  /* Private */

  private[inject] def callPostStartupCallbacks(injector: Injector) {
    if (postStartupFunctions.nonEmpty) {
      info("Calling PostStartup methods in " + this.getClass.getSimpleName)
    }
    postStartupFunctions foreach {_(injector)}
  }

  private[inject] def callShutdownCallbacks() {
    if (shutdownFunctions.nonEmpty) {
      info("Calling Shutdown methods in " + this.getClass.getSimpleName)
    }
    shutdownFunctions foreach {_()}
  }

  /* Copying stacktrace hacks found in scalaguice's ScalaModule.scala */
  private def createBuilder[T: Manifest](annotationOpt: Option[Annotation] = None, singleton: Boolean = true) = {
    val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))
    val builder = mybinder bind typeLiteral[T]

    /* Set as singleton */
    if (singleton) {
      builder.in(annotation[Singleton])
    }

    /* Set annotation if specified */
    for (annot <- annotationOpt) {
      builder.annotatedWith(annot)
    }

    builder
  }
}