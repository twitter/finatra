package com.twitter.inject.app

import com.google.inject.Module
import com.google.inject.Stage
import com.twitter.app.Flag
import com.twitter.app.Flaggable
import com.twitter.app.lifecycle.Event.AfterPostWarmup
import com.twitter.app.lifecycle.Event.BeforePostWarmup
import com.twitter.app.lifecycle.Event.PostInjectorStartup
import com.twitter.app.lifecycle.Event.PostWarmup
import com.twitter.app.lifecycle.Event.Warmup
import com.twitter.inject.Injector
import com.twitter.inject.InjectorModule
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.app.console.ConsoleWriter
import com.twitter.inject.app.internal.ConsoleWriterModule
import com.twitter.inject.app.internal.InstalledModules
import com.twitter.inject.app.internal.Modules
import com.twitter.util.logging.Logging
import com.twitter.util.logging.Slf4jBridge
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/** AbstractApp for usage from Java */
abstract class AbstractApp extends App {

  /**
   * A Java-friendly method for creating a named [[Flag]].
   *
   * @param name the name of the [[Flag]].
   * @param default a default value for the [[Flag]] when no value is given as an application
   *                argument.
   * @param help the help text explaining the purpose of the [[Flag]].
   * @return the created [[Flag]].
   */
  final def createFlag[T](
    name: String,
    default: T,
    help: String,
    flaggable: Flaggable[T]
  ): Flag[T] =
    flag.create(name, default, help, flaggable)

  /**
   * A Java-friendly way to create a "mandatory" [[Flag]]. "Mandatory" flags MUST have a value
   * provided as an application argument (as they have no default value to be used).
   *
   * @param name the name of the [[Flag]].
   * @param help the help text explaining the purpose of the [[Flag]].
   * @param usage a string describing the type of the [[Flag]], i.e.: Integer.
   * @return the created [[Flag]].
   */
  final def createMandatoryFlag[T](
    name: String,
    help: String,
    usage: String,
    flaggable: Flaggable[T]
  ): Flag[T] =
    flag.createMandatory(name, help, usage, flaggable)

  /**
   * Called prior to application initialization.
   */
  def onInit(): Unit = ()

  /**
   * Called before the `main` method.
   */
  def preMain(): Unit = ()

  /**
   * Called after the `main` method.
   */
  def postMain(): Unit = ()

  /**
   * Called prior to application exiting.
   */
  def onExit(): Unit = ()

  /**
   * Called prior to application exiting after `onExit`.
   */
  def onExitLast(): Unit = ()

  init(onInit())
  premain(preMain())
  postmain(postMain())
  onExit(onExit())
  onExitLast(onExitLast())
}

/**
 * A [[com.twitter.app.App]] that supports injection and [[com.twitter.inject.TwitterModule]] modules.
 *
 * It is not expected that you override @Lifecycle methods. If you do, take care to ensure that you
 * call the super implementation, otherwise critical lifecycle set-up may not occur causing your application
 * to either function improperly or outright fail.
 *
 * Typically, you will only need to interact with the following methods:
 *   run -- callback executed after the injector is created and all @Lifecycle methods have completed.
 */
trait App extends com.twitter.app.App with Slf4jBridge with Logging {

  /**
   * All modules composed within this application (including modules referenced only from
   * other modules).
   *
   * @note `required` modules appends "framework" modules first as they map to framework
   *       "defaults" which users can override.
   *       `override` modules appends "framework" modules last as they map to the BindDSL
   *       for testing.
   */
  private[this] lazy val appModules: Modules =
    new Modules(
      required =
        (frameworkModules ++ modules ++ javaModules.asScala).toSeq, // user modules should replace framework
      overrides =
        overrideModules ++ javaOverrideModules.asScala ++ frameworkOverrideModules // framework should replace user modules
    )

  /* Mutable State */

  private val frameworkModules: ArrayBuffer[Module] =
    ArrayBuffer(InjectorModule, ConsoleWriterModule)
  private val frameworkOverrideModules: ArrayBuffer[Module] = ArrayBuffer()

  private[inject] var stage: Stage = Stage.PRODUCTION
  private[this] var installedModules: InstalledModules = _

  /* Lifecycle */

  init {
    info("Process started")

    /* Register all flags from Modules */
    appModules.addFlags(flag)
  }

  /** DO NOT BLOCK */
  def main(): Unit = {
    observe(PostInjectorStartup) {
      installedModules = loadModules()
      installedModules.postInjectorStartup()
      postInjectorStartup()
    }

    observe(Warmup) {
      info("Warming up.")
      warmup()
    }

    observe(BeforePostWarmup) {
      beforePostWarmup()
    }
    observe(PostWarmup) {
      postWarmup()
    }
    observe(AfterPostWarmup) {
      afterPostWarmup()
      installedModules.postWarmupComplete()
    }

    /* Register close and shutdown of InstalledModules */
    registerInstalledModulesExits()

    info(s"$name started.")

    val consoleWriter = injector.instance[ConsoleWriter]
    consoleWriter.let { // use our installed output streams for this application
      /* Execute callback for further configuration or to start long-running background processes */
      startApplication()
    }
  }

  /* Public */

  def injector: Injector = {
    if (installedModules == null)
      throw new Exception("injector is not available before main() is called")
    else
      installedModules.injector
  }

  /* Protected */

  /**
   * @inheritdoc
   *
   * @note It is HIGHLY recommended that this value remains 'true'. This value SHOULD NOT be
   *       changed to 'false' without a very good reason.This method only remains overridable for
   *       legacy reasons.
   */
  override protected def failfastOnFlagsNotParsed: Boolean = true

  /**
   * Production modules.
   *
   * @note Java users should prefer [[javaModules]].
   */
  protected def modules: Seq[Module] = Seq()

  /** Production modules from Java. */
  protected def javaModules: java.util.Collection[Module] = new java.util.ArrayList[Module]()

  /**
   * ONLY INTENDED FOR USE IN TESTING.
   *
   * Override modules which redefine production bindings (only use overrideModules during testing)
   * If you think you need this in your main server you are most likely doing something incorrectly.
   */
  protected def overrideModules: Seq[Module] = Seq()

  /**
   * ONLY INTENDED FOR USE IN TESTING.
   *
   * Override modules from Java which redefine production bindings (only use overrideModules during testing)
   * If you think you need this in your main server you are most likely doing something incorrectly.
   */
  protected def javaOverrideModules: java.util.Collection[Module] =
    new java.util.ArrayList[Module]()

  /**
   * ONLY INTENDED FOR USE BY THE FRAMEWORK.
   *
   * Default modules can be overridden in production by overriding methods in your App or Server.
   *
   * We take special care to make sure the module is not null, since a common bug
   * is overriding the default methods using a val instead of a def
   */
  protected[twitter] def addFrameworkModule(module: Module): Unit = {
    assert(
      module != null,
      "Module cannot be null. If you are overriding a default module, " +
        "override it with 'def' instead of 'val'"
    )
    frameworkModules += module
  }

  /** ONLY INTENDED FOR USE BY THE FRAMEWORK. */
  protected[twitter] def addFrameworkModules(modules: Module*): Unit = {
    modules.foreach(addFrameworkModule)
  }

  /** ONLY INTENDED FOR USE BY THE FRAMEWORK. */
  protected[inject] def addFrameworkOverrideModules(modules: Module*): Unit = {
    frameworkOverrideModules ++= modules
  }

  /** ONLY INTENDED FOR USE BY THE FRAMEWORK. */
  protected[inject] def loadModules(): InstalledModules = {
    appModules.install(
      flags = flag,
      stage = stage
    )
  }

  /** Method to be called after injector creation */
  @Lifecycle
  protected def postInjectorStartup(): Unit = {}

  /** Callback method run before postWarmup */
  protected def warmup(): Unit = {}

  /** Method to be called after successful warmup but before application initialization */
  @Lifecycle
  protected def beforePostWarmup(): Unit = {}

  /** Method to be called after successful warmup */
  @Lifecycle
  protected def postWarmup(): Unit = {}

  /** Method to be be called after port warmup */
  @Lifecycle
  protected def afterPostWarmup(): Unit = {}

  /**
   * Callback method executed after the injector is created and all
   * lifecycle methods have fully completed.
   *
   * The app is signaled as STARTED prior to the execution of this
   * callback as all lifecycle methods have successfully completed.
   *
   * This method can be used to start long-lived processes that run in
   * separate threads from the main() thread. It is expected that you manage
   * these threads manually, e.g., by using a [[com.twitter.util.FuturePool]].
   *
   * Any exceptions thrown in this method will result in the app exiting.
   */
  protected def run(): Unit = {}

  /* Private */

  // Closing will be performing in parallel
  private[this] def registerInstalledModulesExits(): Unit = {
    val funcs = installedModules.shutdown() ++ installedModules.close()
    funcs.foreach { fn =>
      onExit(fn.apply())
    }
  }

  private def startApplication(): Unit = {
    try {
      run()
    } catch {
      case t: Throwable =>
        // we make sure to log a useful error message when an exception is thrown
        error(s"Error in ${this.getClass.getName}#run. ${t.getMessage}", t)
        throw t
    }
  }
}
