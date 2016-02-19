package com.twitter.inject.app

import com.google.inject.{Module, Stage}
import com.twitter.app.{App => TwitterUtilApp}
import com.twitter.inject.app.internal.InstalledModules
import com.twitter.inject.app.internal.InstalledModules.findModuleFlags
import com.twitter.inject.{Injector, InjectorModule, Logging}
import com.twitter.util.{Future, Time}
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

/** AbstractApp for usage from Java */
abstract class AbstractApp extends App

trait App extends TwitterUtilApp with Logging {

  private[inject] lazy val requiredModules = modules ++ javaModules.asScala ++ frameworkModules

  /* Mutable State */

  private val frameworkModules: ArrayBuffer[Module] = ArrayBuffer(InjectorModule)
  private val frameworkOverrideModules: ArrayBuffer[Module] = ArrayBuffer()

  private[inject] var runAppMain: Boolean = true
  private[inject] var appStarted: Boolean = false
  private[inject] var guiceStage: Stage = Stage.PRODUCTION
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

  def main() {
    installedModules = loadModules()
    installedModules.postStartup()
    postStartup()

    info("Warming up.")
    warmup()
    beforePostWarmup()
    postWarmup()
    afterPostWarmup()

    info("App started.")
    setAppStarted(true)

    onExit {
      installedModules.shutdown()
    }

    callAppMain()
  }

  /* Public */

  def injector: Injector = {
    if (installedModules == null)
      throw new Exception("injector is not available before main() is called")
    else
      installedModules.injector
  }

  /**
   * Callback method executed after the injector is created and warmup has fully completed.
   * Note: Not intended for use when using HttpServer or ThriftServer
   */
  def appMain() {
  }

  /* Protected */

  /** Production Guice modules */
  protected def modules: Seq[Module] = Seq()

  /** Production Guice modules from Java */
  protected def javaModules: java.util.Collection[Module] = new java.util.ArrayList[Module]()

  /** Override Guice modules which redefine production bindings (Note: Only use overrideModules during testing) */
  protected def overrideModules: Seq[Module] = Seq()

  /** Override Guice modules from Java which redefine production bindings (Note: Only use overrideModules during testing) */
  protected def javaOverrideModules: java.util.Collection[Module] = new java.util.ArrayList[Module]()

  /**
   * Default modules can be overridden in production by overriding methods in your App or Server
   * We take special care to make sure the module is not null, since a common bug
   * is overriding the default methods using a val instead of a def
   */
  protected def addFrameworkModule(module: Module) {
    assert(
      module != null,
      "Module cannot be null. If you are overriding a default module, " +
      "override it with 'def' instead of 'val'")
    frameworkModules += module
  }

  protected def addFrameworkModules(modules: Module*) {
    modules foreach addFrameworkModule
  }

  protected[inject] def addFrameworkOverrideModules(modules: Module*) {
    frameworkOverrideModules ++= modules
  }

  protected[inject] def setAppStarted(value: Boolean) {
    appStarted = value
  }

  /** Method to be called after injector creation */
  protected def postStartup() {
  }

  /** Warmup method to be called before postWarmup */
  protected def warmup() {
  }

  /** Method to be called after successful warmup */
  protected def postWarmup() {
  }

  protected def beforePostWarmup() {
  }

  protected def afterPostWarmup() {
  }

  protected[inject] def loadModules() = {
    InstalledModules.create(
      flags = flag.getAll(includeGlobal = false).toSeq,
      modules = requiredModules,
      overrideModules = overrideModules ++ javaOverrideModules.asScala ++ frameworkOverrideModules,
      stage = guiceStage)
  }

  /* Private */

  private def callAppMain() {
    if (runAppMain) {
      try {
        appMain()
      } catch {
        case e: Throwable =>
          error("Error in appMain", e)
          throw e
      }
    }
  }
}
