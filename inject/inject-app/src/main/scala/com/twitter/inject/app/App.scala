package com.twitter.inject.app

import com.google.inject.{Module, Stage}
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.app.internal.InstalledModules
import com.twitter.inject.app.internal.InstalledModules.findModuleFlags
import com.twitter.inject.{Injector, InjectorModule, Logging}
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/** AbstractApp for usage from Java */
abstract class AbstractApp extends App

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
trait App
  extends com.twitter.app.App
  with Logging {

  private[inject] lazy val requiredModules = modules ++ javaModules.asScala ++ frameworkModules

  /* Mutable State */

  private val frameworkModules: ArrayBuffer[Module] = ArrayBuffer(InjectorModule)
  private val frameworkOverrideModules: ArrayBuffer[Module] = ArrayBuffer()

  private[inject] var started: Boolean = false
  private[inject] var stage: Stage = Stage.PRODUCTION
  private var installedModules: InstalledModules = _

  /* Lifecycle */

  init {
    info("Process started")

    /* Get all module flags */
    val allModules = requiredModules ++ overrideModules ++ javaOverrideModules.asScala ++ frameworkOverrideModules
    val allModuleFlags = findModuleFlags(allModules)

    /* Parse all flags */
    allModuleFlags foreach flag.add
  }

  /** DO NOT BLOCK */
  def main(): Unit = {
    installedModules = loadModules()

    postInjectorStartup()
    installedModules.postInjectorStartup()

    info("Warming up.")
    warmup()
    beforePostWarmup()
    postWarmup()
    afterPostWarmup()
    installedModules.postWarmupComplete()

    onExit {
      installedModules.shutdown()
    }

    /* Lifecycle is complete, mark the server as started. */
    setAppStarted(true)
    info(s"$name started.")
    /* Execute callback for further configuration or to start long-running background processes */
    startApplication()
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
  protected def run(): Unit = {
  }

  /** Production modules */
  protected def modules: Seq[Module] = Seq()

  /** Production modules from Java */
  protected def javaModules: java.util.Collection[Module] = new java.util.ArrayList[Module]()

  /** Override modules which redefine production bindings (only use overrideModules during testing) */
  protected def overrideModules: Seq[Module] = Seq()

  /** Override modules from Java which redefine production bindings (only use overrideModules during testing) */
  protected def javaOverrideModules: java.util.Collection[Module] = new java.util.ArrayList[Module]()

  /**
   * ONLY INTENDED FOR USE BY THE FRAMEWORK.
   *
   * Default modules can be overridden in production by overriding methods in your App or Server.
   *
   * We take special care to make sure the module is not null, since a common bug
   * is overriding the default methods using a val instead of a def
   */
  protected def addFrameworkModule(module: Module): Unit = {
    assert(
      module != null,
      "Module cannot be null. If you are overriding a default module, " +
      "override it with 'def' instead of 'val'")
    frameworkModules += module
  }

  /** Only intended for use by the framework */
  protected def addFrameworkModules(modules: Module*): Unit = {
    modules foreach addFrameworkModule
  }

  /** Only intended for use by the framework */
  protected[inject] def addFrameworkOverrideModules(modules: Module*): Unit = {
    frameworkOverrideModules ++= modules
  }

  /** Only intended for use by the framework */
  protected[inject] def loadModules() = {
    InstalledModules.create(
      flags = flag.getAll(includeGlobal = false).toSeq,
      modules = requiredModules,
      overrideModules = overrideModules ++ javaOverrideModules.asScala ++ frameworkOverrideModules,
      stage = stage)
  }

  /** Method to be called after injector creation */
  @Lifecycle
  protected def postInjectorStartup(): Unit = {
  }

  /** Warmup method to be called before postWarmup */
  @Lifecycle
  protected def warmup(): Unit = {
  }

  /** Method to be called after successful warmup but before application initialization */
  @Lifecycle
  protected def beforePostWarmup(): Unit = {
  }

  /** Method to be called after successful warmup */
  @Lifecycle
  protected def postWarmup(): Unit = {
  }

  /** Method to be be called after port warmup */
  @Lifecycle
  protected def afterPostWarmup(): Unit = {
  }

  /* Private */

  private def setAppStarted(value: Boolean): Unit = {
    started = value
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
