package com.twitter.finatra.guice

import com.google.inject.Module
import com.twitter.app.App
import com.twitter.finatra.guice.FinatraInjector.findModuleFlags
import com.twitter.finatra.modules.LoadedStatsModule
import com.twitter.finatra.utils.Logging
import scala.collection.mutable.ArrayBuffer


trait GuiceApp extends App with Logging {

  private[finatra] lazy val requiredModules = modules ++ frameworkModules

  /* Mutable State */

  private val frameworkModules: ArrayBuffer[Module] = ArrayBuffer(statsModule)
  @transient private[finatra] var runAppMain: Boolean = true
  @transient private[finatra] var postWarmupComplete: Boolean = false
  @transient private var _injector: FinatraInjector = _

  /* Public */

  def injector: FinatraInjector = {
    if (_injector == null)
      throw new Exception("injector is not available before main() is called")
    else
      _injector
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
    _injector = createInjector()
    postStartup()
    injector.postStartup()

    warmup()
    postWarmup()
    injector.postWarmup()
    postWarmupComplete = true
    info("Warmup & PostWarmup Finished.")

    callAppMain()

    onExit {
      injector.shutdown()
    }
  }

  /* Public */

  /**
   * Callback method executed after the Guice injector is created and warmup has fully completed.
   */
  def appMain() {
  }

  /* Protected */

  /** Production Guice modules */
  protected def modules: Seq[Module] = Seq()

  /** Override Guice modules which redefine production bindings */
  protected def overrideModules: Seq[Module] = Seq()

  // TODO: Replace the need for this method with Guice v4 OptionalBinder
  // http://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/multibindings/OptionalBinder.html
  protected def statsModule: Module = LoadedStatsModule
  
  protected def addFrameworkModules(modules: Module*) {
    frameworkModules ++= modules
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

  protected[finatra] def createInjector() = {
    FinatraInjector(
      flags = flag.getAll(includeGlobal = false).toSeq,
      modules = requiredModules,
      overrideModules = overrideModules)
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
