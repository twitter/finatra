package com.twitter.finatra.guice

import com.google.inject.Module
import com.twitter.app.App
import com.twitter.finatra.guice.FinatraInjector.findModuleFlags
import com.twitter.finatra.modules.LoadedStatsModule
import com.twitter.finatra.utils.{Logging, StringUtils}
import scala.collection.mutable.ArrayBuffer


trait GuiceApp extends App with Logging {

  private[finatra] lazy val requiredModules = modules ++ frameworkModules

  /* Mutable State */

  private val frameworkModules: ArrayBuffer[Module] = ArrayBuffer(LoadedStatsModule)
  @transient private[finatra] var postWarmedUp: Boolean = false
  @transient private[finatra] var autoRunAppMain: Boolean = true
  @transient private var _injector: FinatraInjector = _

  /* Public */

  def injector: FinatraInjector = {
    if (_injector == null)
      throw new Exception("injector is not available until after premain")
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

  premain {
    _injector = createInjector()
  }

  def main() {
    postStartup()
    injector.postStartup()

    warmup()
    postWarmup()
    injector.postWarmup()
    postWarmedUp = true
    info("Warmup & PostWarmup Finished.")

    callAppMain()

    onExit {
      injector.shutdown()
    }
  }

  /* Public */

  /** Callback method called after the Guice injector is created */
  def appMain() {
  }

  /* Protected */

  /** Production Guice modules */
  protected def modules: Seq[Module] = Seq()

  /** Override Guice modules which redefine production bindings */
  protected def overrideModules: Seq[Module] = Seq()

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

  /* Overrides */

  override val name =
    StringUtils.simpleName(getClass)

  /* Private */

  private[finatra] def createInjector() = {
    FinatraInjector(
      flags = flag.getAll(includeGlobal = false).toSeq,
      modules = requiredModules,
      overrideModules = overrideModules)
  }

  private def callAppMain() {
    if (autoRunAppMain) {
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
