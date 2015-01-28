package com.twitter.finatra.guice

import com.google.inject.{Module, Stage}
import com.twitter.app.App
import com.twitter.finatra.guice.FinatraInstalledModules.findModuleFlags
import com.twitter.finatra.modules.{FinatraInjectorModule, LoadedStatsModule}
import com.twitter.finatra.utils.{ResolverUtils, Logging}
import scala.collection.mutable.ArrayBuffer


trait GuiceApp extends App with Logging {

  private[finatra] lazy val requiredModules = modules ++ frameworkModules

  /* Mutable State */

  @transient private val frameworkModules: ArrayBuffer[Module] = ArrayBuffer(
    FinatraInjectorModule,
    statsModule)

  @transient private[finatra] var runAppMain: Boolean = true
  @transient private[finatra] var postWarmupComplete: Boolean = false
  @transient private[finatra] var guiceStage: Stage = Stage.PRODUCTION
  @transient private var installedModules: FinatraInstalledModules = _

  /* Public */

  /* TODO: Remove once opensource twitter-util with nonExitingMain is released */
  override protected def exitOnError(reason: String) {
    error(reason)
    close()
    throw new Error(reason)
  }

  def injector: FinatraInjector = {
    if (installedModules == null)
      throw new Exception("injector is not available before main() is called")
    else
      installedModules.injector
  }

  /* Lifecycle */

  init {
    info("Process started")

    /* Get all modules */
    val allModuleFlags = findModuleFlags(requiredModules ++ overrideModules)

    /* Parse all flags */
    allModuleFlags foreach flag.add
  }

  def main() {
    installedModules = loadModules()
    postStartup()
    installedModules.postStartup()

    if (resolveFinagleClientsBeforeWarmup) {
      ResolverUtils.waitUntilAllClientsAreResolved()
    }
    warmup()
    postWarmup()
    installedModules.postWarmup()
    postWarmupComplete = true
    info("Warmup & PostWarmup Finished.")

    onExit {
      installedModules.shutdown()
    }

    callAppMain()
  }

  /* Public */

  /**
   * Callback method executed after the Guice injector is created and warmup has fully completed.
   * Note: Not intended for use when using FinatraServer
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

  /** Method to be called after injector creation */
  protected def postStartup() {
  }

  /** Resolve all Finagle clients before warmup method called */
  protected def resolveFinagleClientsBeforeWarmup = false

  /** Warmup method to be called before postWarmup */
  protected def warmup() {
  }

  /** Method to be called after successful warmup */
  protected def postWarmup() {
  }

  protected[finatra] def loadModules() = {
    FinatraInstalledModules.create(
      flags = flag.getAll(includeGlobal = false).toSeq,
      modules = requiredModules,
      overrideModules = overrideModules,
      stage = guiceStage)
  }

  @deprecated("use loadModules().injector", "now")
  protected[finatra] def createInjector() = {
    loadModules().injector
  }

  // TODO: Replace the need for this method with Guice v4 OptionalBinder
  // http://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/multibindings/OptionalBinder.html
  protected def statsModule: Module = LoadedStatsModule

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
