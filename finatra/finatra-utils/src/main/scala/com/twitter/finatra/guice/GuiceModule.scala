package com.twitter.finatra.guice

import com.google.inject._
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.matcher.Matchers
import com.google.inject.name.Names
import com.google.inject.spi.TypeConverter
import com.twitter.app.{Flag, FlagFactory, Flaggable}
import com.twitter.finatra.utils.Logging
import net.codingwell.scalaguice.ScalaModule.ScalaAnnotatedBindingBuilder
import net.codingwell.scalaguice._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe.TypeTag

abstract class GuiceModule
  extends AbstractModule
  with Logging {

  private def binderAccess = super.binder

  /* Mutable State */
  private val postStartupFunctions = mutable.Buffer[() => Unit]()
  private val postWarmupFunctions = mutable.Buffer[() => Unit]()
  private val shutdownFunctions = mutable.Buffer[() => Unit]()
  protected[guice] val flags = ArrayBuffer[Flag[_]]()

  /* Protected */

  /** Create a flag and add it to the modules flags list */
  protected def flag[T: Flaggable](name: String, default: T, help: String): Flag[T] = {
    val flag = FlagFactory.create(name, default, help)
    flags += flag
    flag
  }

  protected def flag[T: Flaggable : TypeTag](name: String, help: String): Flag[T] = {
    val flag = FlagFactory.create[T](name, help)
    flags += flag
    flag
  }

  /** Additional modules to be composed into this module
    * NOTE: This Seq of modules is used instead of the standard Guice 'install' method */
  protected[guice] def modules: Seq[Module] = Seq()

  protected def bindAssistedFactory[T: Manifest] = {
    super.install(
      new FactoryModuleBuilder().build(manifest[T].erasure))
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

  protected def bindSingleton[T: Manifest](name: String): ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder = createBuilder(nameOpt = Some(name))
      override val self = builder
    }
  }

  protected def bind[T: Manifest](name: String): ScalaAnnotatedBindingBuilder[T] = {
    new ScalaAnnotatedBindingBuilder[T] {
      val builder = createBuilder(nameOpt = Some(name), singleton = false)
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

  /*
   * Protected Lifecycle
   * TODO: In future eliminate the need for these lifecycle methods by integrating
   * with Onami-Lifecycle or Governator for support of @PostConstruct, @PreDestroy, and @Warmup annotations
   */

  /**
   * Invoke `singleton func` after Guice injector is started
   * NOTE: This method should only be called from a @Singleton 'provides' method to avoid registering
   * multiple startup hooks every time an object is created.
   */
  protected def singletonStartup(func: => Unit) {
    postStartupFunctions += (() => func)
  }

  /**
   * Invoke `singleton func` after Guice injector is started
   * NOTE: This method should only be called from a @Singleton 'provides' method to avoid registering
   * multiple startup hooks every time an object is created.
   */
  protected def singletonPostWarmup(func: => Unit) {
    postWarmupFunctions += (() => func)
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

  override protected def configure() {} // Provide default configure method so Module's using @Provider don't need an empty configure method

  override protected def install(module: Module) {
    throw new Exception("Install not supported. Please place modules override val modules = Seq(module1, module2, ...)")
  }

  /* Private */

  private[guice] def callPostStartupCallbacks() {
    postStartupFunctions foreach {_()}
    if (postStartupFunctions.nonEmpty) {
      info("Calling PostStartup methods in " + this)
    }
  }

  private[guice] def callPostWarmupCallbacks() {
    postWarmupFunctions foreach {_()}
    if (postWarmupFunctions.nonEmpty) {
      info("Calling PostWarmup methods in " + this)
    }
  }

  private[guice] def callShutdownCallbacks() {
    shutdownFunctions foreach {_()}
    if (shutdownFunctions.nonEmpty) {
      info("Calling Shutdown methods in " + this)
    }
  }

  /* Copying stacktrace hacks found in scalaguice's ScalaModule.scala */
  private def createBuilder[T: Manifest](nameOpt: Option[String] = None, singleton: Boolean = true) = {
    val mybinder = binderAccess.withSource((new Throwable).getStackTrace()(3))
    val builder = mybinder bind typeLiteral[T]

    /* Set as singleton */
    if (singleton) {
      builder.in(annotation[Singleton])
    }

    /* Set name annotation if specified */
    for (name <- nameOpt) {
      val nameAnnotation = Names.named(name)
      builder.annotatedWith(nameAnnotation)
    }

    builder
  }
}