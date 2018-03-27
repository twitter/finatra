package com.twitter.inject.app

import com.google.inject.{Module, Stage}
import com.twitter.app.{Flag, FlagParseException, FlagUsageError, Flags}
import com.twitter.inject.Injector
import com.twitter.inject.app.internal.InstalledModules

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
  def apply: TestInjector = apply(modules = Seq())

  /**
   * Create a new TestInjector over the given list of [[com.google.inject.Module]]
   *
   * @param modules - a variable list of [[com.google.inject.Module]]
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  def apply(modules: Module*): TestInjector = {
    apply(modules = modules)
  }

  /**
   * Create a new TestInjector with the specified params.
   *
   * @param modules - a list of [[com.google.inject.Module]]
   * @param flags - a String Map of flag arguments to set on the injector, default is empty.
   * @param overrideModules - a list of [[com.google.inject.Module]] to use as overrides, default is empty.
   * @param stage - the [[com.google.inject.Stage]] to use, default is DEVELOPMENT.
   * @return a new [[TestInjector]]
   *
   * @see https://twitter.github.io/finatra/user-guide/testing/index.html#integration-tests
   */
  def apply(
    modules: Seq[Module],
    flags: Map[String, String] = Map.empty,
    overrideModules: Seq[Module] = Seq.empty,
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
  flags: Map[String, String] = Map.empty,
  overrideModules: Seq[Module] = Seq.empty,
  stage: Stage = Stage.DEVELOPMENT
) extends BindDSL {

  /* Fields */

  private[this] val flag: Flags =
    new Flags(this.getClass.getSimpleName, includeGlobal = true, failFastUntilParsed = true)

  /* Mutable state */

  private[this] var overrides: Seq[Module] = overrideModules
  private[this] var starting = false
  private[this] var started = false
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

  /* Protected */

  override final protected def addInjectionServiceModule(module: Module): Unit = {
    if (started) {
      throw new IllegalStateException("Cannot call bind() on a started TestInjector.")
    }
    overrides = overrides :+ module
  }

  /* Private */

  private[this] def start(): Unit = {
    if (!starting && !started) {
      starting = true //mutation

      val moduleFlags = InstalledModules.findModuleFlags(modules ++ overrides)
      moduleFlags.foreach(flag.add)
      parseFlags(flag, flags, moduleFlags)

      underlying = InstalledModules
        .create(
          flags = flag.getAll(includeGlobal = false).toSeq,
          modules = modules,
          overrideModules = overrides,
          stage = stage
        )
        .injector

      started = true //mutation
      starting = false //mutation
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
