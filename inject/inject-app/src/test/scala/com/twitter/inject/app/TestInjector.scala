package com.twitter.inject.app

import com.google.inject.{Injector => UnderlyingInjector}
import com.google.inject.{Key, Module, Stage}
import com.twitter.app.{FlagParseException, FlagUsageError, Flags}
import com.twitter.inject.{Injector, InjectorModule}
import com.twitter.inject.app.internal.{InstalledModules, Modules}
import java.lang.annotation.Annotation
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.varargs
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * An [[Injector]] usable for testing. This injector can be used for constructing a minimal object
 * graph for use in integration tests. The TestInjector supports modules, flags, override modules
 * and a `bind[T]` DSL (for easily replacing bound instances without using an override module).
 *
 * Usage is typically through extension of the com.twitter.inject.IntegrationTest
 * (or com.twitter.inject.IntegrationTestMixin) which expects a defined [[Injector]] which can be
 * created by [[TestInjector#create]].
 *
 * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
 */
object TestInjector {

  final class Builder(
    modules: Seq[Module],
    flags: Map[String, String] = Map.empty[String, String],
    overrideModules: Seq[Module] = Seq.empty[Module],
    stage: Stage = Stage.DEVELOPMENT)
      extends BindDSL {

    /* Mutable state */

    private[this] var overrides: Seq[Module] = overrideModules

    /* Public */

    /**
     * Creates a new [[TestInjector]] from this builder.
     *
     *
     * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
     *     Integration Tests]]
     */
    def create(): TestInjector = {
      // Add the `InjectorModule` to mirror the behavior in `c.t.inject.app.App` which adds it
      // as a framework module. This ensures the TestInjector has the same baseline of modules
      // as a `c.t.inject.app.App`.

      val flag: Flags =
        new Flags(this.getClass.getSimpleName, includeGlobal = true, failFastUntilParsed = true)

      val injectorModules = new Modules(Seq(InjectorModule) ++ modules, overrides)
      injectorModules.addFlags(flag)
      parseFlags(flag, flags)

      new TestInjector(injectorModules.install(flags = flag, stage = stage))
    }

    /** For Java compatibility */
    def newInstance(): TestInjector = create()

    // java-forwarder methods
    override final def bindClass[T](clazz: Class[T], instance: T): this.type =
      super.bindClass[T](clazz, instance)

    // java-forwarder methods
    override final def bindClass[T](
      clazz: Class[T],
      annotation: Annotation,
      instance: T
    ): this.type =
      super.bindClass[T](clazz, annotation, instance)

    // java-forwarder methods
    override final def bindClass[T, Ann <: Annotation](
      clazz: Class[T],
      annotationClazz: Class[Ann],
      instance: T
    ): this.type =
      super.bindClass[T, Ann](clazz, annotationClazz, instance)

    // java-forwarder methods
    override final def bindClass[T, U <: T](clazz: Class[T], instanceClazz: Class[U]): this.type =
      super.bindClass[T, U](clazz, instanceClazz)

    // java-forwarder methods
    override final def bindClass[T, U <: T](
      clazz: Class[T],
      annotation: Annotation,
      instanceClazz: Class[U]
    ): this.type =
      super.bindClass[T, U](clazz, annotation, instanceClazz)

    // java-forwarder methods
    override final def bindClass[T, Ann <: Annotation, U <: T](
      clazz: Class[T],
      annotationClazz: Class[Ann],
      instanceClazz: Class[U]
    ): this.type =
      super.bindClass[T, Ann, U](clazz, annotationClazz, instanceClazz)

    /* Protected */

    final protected def addInjectionServiceModule(module: Module): Unit = {
      overrides = overrides :+ module
    }

    /* Private */

    private[this] def parseFlags(
      flag: Flags,
      flags: Map[String, String]
    ): Unit = {
      /* Parse all flags with incoming supplied flag values */
      val args = flags.map { case (k, v) => s"-$k=$v" }.toArray

      flag.parseArgs(args) match {
        case Flags.Help(usage) =>
          throw FlagUsageError(usage)
        case Flags.Error(reason) =>
          throw FlagParseException(reason)
        case _ => // nothing
      }
    }
  }

  /* Public */

  /** Create a new TestInjector */
  def apply: Builder = apply(modules = Seq.empty[Module])

  /**
   * Create a new TestInjector over the given list of [[com.google.inject.Module]].
   *
   * @param modules a variable list of [[com.google.inject.Module]].
   *
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  @varargs def apply(modules: Module*): Builder = {
    apply(modules = modules)
  }

  /**
   * Create a new TestInjector over the given list of [[com.google.inject.Module]]. This is meant
   * to be used from Java.
   *
   * @param javaModules a list of [[com.google.inject.Module]].
   *
   * @note Scala users should prefer `apply(Seq[Module])`
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  def apply(javaModules: java.util.Collection[Module]): Builder = {
    apply(javaModules.asScala.toSeq)
  }

  /**
   * Create a new TestInjector over the given list of [[com.google.inject.Module]] and map of flag
   * String key/values. This is meant to be used from Java.
   *
   * @param javaModules a list of [[com.google.inject.Module]].
   * @param javaFlags a String Map of flag arguments to set on the injector.
   *
   * @note Scala users should prefer the version which accepts Scala collections.
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  def apply(
    javaModules: java.util.Collection[Module],
    javaFlags: java.util.Map[String, String]
  ): Builder =
    apply(javaModules.asScala.toSeq, javaFlags.asScala.toMap)

  /**
   * Create a new TestInjector over the given list of [[com.google.inject.Module]], a map of flag
   * String key/values, and a list of override [[com.google.inject.Module]]. This is meant to be
   * used from Java.
   *
   * @param javaModules a list of [[com.google.inject.Module]].
   * @param javaFlags a String Map of flag arguments to set on the injector.
   * @param javaOverrideModules a list of [[com.google.inject.Module]] to use as overrides.
   *
   * @note Scala users should prefer the version which accepts Scala collections.
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  def apply(
    javaModules: java.util.Collection[Module],
    javaFlags: java.util.Map[String, String],
    javaOverrideModules: java.util.Collection[Module]
  ): Builder =
    apply(javaModules.asScala.toSeq, javaFlags.asScala.toMap, javaOverrideModules.asScala.toSeq)

  /**
   * Create a new TestInjector with the specified params. This is meant to be used from Java.
   *
   * @param javaModules a list of [[com.google.inject.Module]].
   * @param javaFlags a String Map of flag arguments to set on the injector.
   * @param javaOverrideModules a list of [[com.google.inject.Module]] to use as overrides.
   * @param stage the [[com.google.inject.Stage]] to use.
   *
   * @note Scala users should prefer the version which accepts Scala collections.
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  def apply(
    javaModules: java.util.Collection[Module],
    javaFlags: java.util.Map[String, String],
    javaOverrideModules: java.util.Collection[Module],
    stage: Stage
  ): Builder =
    apply(
      javaModules.asScala.toSeq,
      javaFlags.asScala.toMap,
      javaOverrideModules.asScala.toSeq,
      stage)

  /**
   * Create a new TestInjector with the specified params. This is meant to be used from Scala.
   *
   * @param modules a list of [[com.google.inject.Module]]
   * @param flags a String Map of flag arguments to set on the injector, default is empty.
   * @param overrideModules a list of [[com.google.inject.Module]] to use as overrides, default is empty.
   * @param stage the [[com.google.inject.Stage]] to use, default is DEVELOPMENT.
   *
   * @note Java users should prefer the version which accepts Java collections.
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  def apply(
    modules: Seq[Module],
    flags: Map[String, String] = Map.empty[String, String],
    overrideModules: Seq[Module] = Seq.empty[Module],
    stage: Stage = Stage.DEVELOPMENT
  ): Builder =
    new Builder(modules, flags, overrideModules, stage)
}

/**
 * An instance of a [[TestInjector]] that can extend [[Injector]] with a couple of bonus methods
 * allowing for manual execution of lifecycle hooks:
 *
 * - `start()`: executes `singletonStartup` and `singletonPostWarmupComplete` callbacks
 * - `close()`: executes `singletonShutdown` and `closeOnExit` callbacks
 *
 * This injector can NOT be used after it's closed. But this injector CAN be used before it's
 * started.
 */
final class TestInjector private (modules: InstalledModules) extends Injector {

  private val closed = new AtomicBoolean(false)
  private val started = new AtomicBoolean(false)

  private def injector(): Injector =
    if (closed.get()) throw new IllegalStateException("Can not use already CLOSED TestInjector")
    else modules.injector

  /**
   * Start this injector. This executes `singletonStartup` and `singletonPostWarmupComplete`
   * callbacks in the caller threads.
   *
   * @note An injector can still be used even if it's not started yet. It's your responsibility to
   *       `start` it before using if that insures proper initialization order for provided
   *       bindings.
   */
  def start(): Unit =
    if (started.compareAndSet(false, true)) {
      modules.postInjectorStartup()
      modules.postWarmupComplete()
    } else {
      throw new IllegalStateException("Can not start already STARTED TestInjector")
    }

  /**
   * Close this injector. This executes `singletonShutdown` and `closeOnExit` callbacks in the
   * caller thread.
   *
   * @note An injector can't be used after it's closed.
   */
  def close(): Unit =
    if (closed.compareAndSet(false, true)) {
      modules.shutdown().foreach(_.apply())
      modules.close().foreach(_.apply())
    } else {
      throw new IllegalStateException("Can not close already CLOSED TestInjector")
    }

  def underlying: UnderlyingInjector = modules.injector.underlying

  def instance[T: TypeTag]: T =
    injector().instance[T]

  def instance[T: TypeTag, Ann <: Annotation: ClassTag]: T =
    injector().instance[T, Ann]

  def instance[T: TypeTag](annotation: Annotation): T =
    injector().instance[T](annotation)

  def instance[T: TypeTag](annotationClazz: Class[_ <: Annotation]): T =
    injector().instance[T](annotationClazz)

  @deprecated(
    "Users should prefer injector.instance[T](java.lang.annotation.Annotation",
    "2017-09-25")
  def instance[T: TypeTag](name: String): T =
    injector().instance[T](name)

  def instance[T](clazz: Class[T]): T =
    injector().instance[T](clazz)

  def instance[T](clazz: Class[T], annotation: Annotation): T =
    injector().instance[T](clazz, annotation)

  def instance[T, Ann <: Annotation](clazz: Class[T], annotationClazz: Class[Ann]): T =
    injector().instance[T, Ann](clazz, annotationClazz)

  def instance[T](key: Key[T]): T =
    injector().instance[T](key)
}
