package com.twitter.inject.app

import com.google.inject.{Module, Stage}
import com.twitter.app.{Flag, FlagParseException, FlagUsageError, Flags}
import com.twitter.inject.{Injector, InjectorModule}
import com.twitter.inject.app.internal.Modules
import java.lang.annotation.Annotation
import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.varargs
import scala.collection.JavaConverters._

/**
 * A [[com.google.inject.Injector]] usable for testing. This injector can be used for
 * constructing a minimal object graph for use in integration tests. The TestInjector
 * supports modules, flags, override modules and a `bind[T]` DSL (for easily replacing
 * bound instances without using an override module).
 *
 * Usage is typically through extension of the com.twitter.inject.IntegrationTest
 * (or com.twitter.inject.IntegrationTestMixin) which expects a defined
 * [[com.google.inject.Injector]] which can be created by [[TestInjector#create]].
 *
 * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
 */
object TestInjector {

  /* Public */

  /** Create a new TestInjector */
  def apply: TestInjector = apply(modules = Seq.empty[Module])

  /**
   * Create a new TestInjector over the given list of [[com.google.inject.Module]].
   *
   * @param modules a variable list of [[com.google.inject.Module]].
   *
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  @varargs def apply(modules: Module*): TestInjector = {
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
  def apply(javaModules: java.util.Collection[Module]): TestInjector = {
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
  ): TestInjector =
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
  ): TestInjector =
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
  ): TestInjector =
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
  ): TestInjector = {
    new TestInjector(modules, flags, overrideModules, stage)
  }
}

/**
 * A [[com.google.inject.Injector]] usable for testing. Note, it is expected that construction
 * of the TestInjector happens within a single thread as private state is mutated on creation.
 *
 * @param modules - a list of [[com.google.inject.Module]]
 * @param flags - a String Map of flag arguments to set on the injector, default is empty.
 * @param overrideModules - a list of [[com.google.inject.Module]] to use as overrides, default is empty.
 * @param stage - the [[com.google.inject.Stage]] to use, default is DEVELOPMENT.
 *
 * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
 */
class TestInjector(
  modules: Seq[Module],
  flags: Map[String, String] = Map.empty[String, String],
  overrideModules: Seq[Module] = Seq.empty[Module],
  stage: Stage = Stage.DEVELOPMENT)
    extends BindDSL {

  /* Fields */

  private[this] val flag: Flags =
    new Flags(this.getClass.getSimpleName, includeGlobal = true, failFastUntilParsed = true)

  private[this] val _started: AtomicBoolean = new AtomicBoolean(false)
  private[inject] def started: Boolean = _started.get

  /* Mutable state */

  private[this] var overrides: Seq[Module] = overrideModules
  private[this] var underlying: Injector = _

  /* Public */

  /**
   * Creates a new [[com.google.inject.Injector]] from this TestInjector.
   * @return a new [[com.google.inject.Injector]].
   *
   * @note Java users: see the more Java-friendly [[TestInjector.newInstance()]] method.
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests Integration Tests]]
   */
  def create: Injector = {
    start()
    underlying
  }

  /** For Java compatibility */
  def newInstance(): Injector = {
    start()
    underlying
  }

  // java-forwarder methods
  override final def bindClass[T](clazz: Class[T], instance: T): this.type =
    super.bindClass[T](clazz, instance)

  // java-forwarder methods
  override final def bindClass[T](clazz: Class[T], annotation: Annotation, instance: T): this.type =
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

  override final protected def addInjectionServiceModule(module: Module): Unit = {
    if (started) {
      throw new IllegalStateException("Cannot call bind() on a started TestInjector.")
    }
    overrides = overrides :+ module
  }

  /* Private */

  private[this] def start(): Unit = {
    if (_started.compareAndSet(false, true)) {
      // Add the `InjectorModule` to mirror the behavior in `c.t.inject.app.App` which adds it
      // as a framework module. This ensures the TestInjector has the same baseline of modules
      // as a `c.t.inject.app.App`.
      val injectorModules = new Modules(Seq(InjectorModule) ++ modules, overrides)
      injectorModules.addFlags(flag)
      parseFlags(flag, flags, injectorModules.moduleFlags)

      underlying = injectorModules
        .install(flags = flag, stage = stage)
        .injector
    }
  }

  private[this] def parseFlags(
    flag: Flags,
    flags: Map[String, String],
    moduleFlags: Seq[Flag[_]]
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
