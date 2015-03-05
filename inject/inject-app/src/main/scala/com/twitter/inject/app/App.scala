package com.twitter.inject.app

import com.google.inject.{Module, Stage}
import com.twitter.app.{App => TwitterUtilApp}
import com.twitter.inject.app.internal.InstalledModules
import com.twitter.inject.app.internal.InstalledModules.findModuleFlags
import com.twitter.inject.{Injector, InjectorModule, Logging}
import scala.collection.mutable.ArrayBuffer


trait App extends TwitterUtilApp with Logging {

  private[inject] lazy val requiredModules = modules ++ frameworkModules

  /* Mutable State */

  private val frameworkModules: ArrayBuffer[Module] = ArrayBuffer(InjectorModule)
  private val frameworkOverrideModules: ArrayBuffer[Module] = ArrayBuffer()

  private[inject] var runAppMain: Boolean = true
  private[inject] var appStarted: Boolean = false
  private[inject] var guiceStage: Stage = Stage.PRODUCTION
  private var installedModules: InstalledModules = _

  /* Public */

  def injector: Injector = {
    if (installedModules == null)
      throw new Exception("injector is not available before main() is called")
    else
      installedModules.injector
  }

  /* Lifecycle */

  init {
    info("Process started")

    /* Get all module flags */
    val allModuleFlags = findModuleFlags(requiredModules ++ overrideModules ++ frameworkOverrideModules)

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
    appStarted = true

    onExit {
      installedModules.shutdown()
    }

    callAppMain()
  }

  /* Public */

  /**
   * Callback method executed after the Guice injector is created and warmup has fully completed.
   * Note: Not intended for use when using HttpServer or ThriftServer
   */
  def appMain() {
  }

  /* Protected */

  /** Production Guice modules */
  protected def modules: Seq[Module] = Seq()

  /** Override Guice modules which redefine production bindings (Note: Only override during testing) */
  protected def overrideModules: Seq[Module] = Seq()

  protected def addFrameworkModules(modules: Module*) {
    frameworkModules ++= modules
  }

  /**
   * Default modules can be overridden in production by overriding methods in your App or Server
   * We take special care to make sure the module is not null, since a common bug
   * is overriding the default methods using a val instead of a def
   */
  protected def addDefaultModule(module: Module) {
    assert(module != null, "Default modules must be overridden with a 'def' instead of a 'val'")
    frameworkModules += module
  }

  protected[inject] def addFrameworkOverrideModules(modules: Module*) {
    frameworkOverrideModules ++= modules
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
      overrideModules = overrideModules ++ frameworkOverrideModules,
      stage = guiceStage)
  }

  @deprecated("use loadModules().injector", "now")
  protected[inject] def createInjector() = {
    loadModules().injector
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
