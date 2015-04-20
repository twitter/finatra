package com.twitter.inject.app.internal

import com.google.inject.util.Modules
import com.google.inject.{Module => GuiceModule, _}
import com.twitter.app.Flag
import com.twitter.inject.{Injector, Logging, TwitterModule}
import scala.collection.JavaConverters._

object InstalledModules {

  /* Public */

  def create(
    flags: Seq[Flag[_]],
    modules: Seq[GuiceModule],
    overrideModules: Seq[GuiceModule],
    stage: Stage = Stage.PRODUCTION): InstalledModules = {

    val allNonOverrideModules = {
      val frameworkModules = Seq(
        FlagsModule.create(flags),
        TwitterTypeConvertersModule)

      val composedModules = modules flatMap findInstalledModules
      modules ++ composedModules ++ frameworkModules
    }

    val allOverrideModules = {
      val composedOverrideModules = overrideModules flatMap findInstalledModules
      overrideModules ++ composedOverrideModules
    }

    val combinedModule =
      Modules.`override`(
        allNonOverrideModules.asJava).
        `with`(allOverrideModules.asJava)

    new InstalledModules(
      injector = Injector(Guice.createInjector(stage, combinedModule)),
      modules =
        allNonOverrideModules ++ allOverrideModules)
  }

  /* Private */

  /**
   * Recursively capture all flags in the GuiceModule object hierarchy.
   *
   * Note: We will not (cannot?) traverse through a normal Guice AbstractModule, to find 'installed' GuiceModules
   */
  private[inject] def findModuleFlags(modules: Seq[GuiceModule]): Seq[Flag[_]] = {
    (modules collect {
      case injectModule: TwitterModule =>
        injectModule.flags ++ findModuleFlags(injectModule.modules)
    }).flatten.distinct
  }

  /** Recursively finds all 'composed' modules */
  private def findInstalledModules(module: GuiceModule): Seq[GuiceModule] = module match {
    case injectModule: TwitterModule =>
      injectModule.modules ++
        (injectModule.modules flatMap findInstalledModules)
    case _ =>
      Seq()
  }
}

case class InstalledModules(
  injector: Injector,
  modules: Seq[GuiceModule])
  extends Logging {

  def postStartup() {
    modules foreach {
      case injectModule: TwitterModule =>
        try {
          injectModule.callPostStartupCallbacks(injector)
        } catch {
          case e: Throwable =>
            error("Startup method error in " + injectModule, e)
            throw e
        }
      case _ =>
    }
  }

  // Note: We don't rethrow so that all modules have a change to shutdown
  def shutdown() {
    modules foreach {
      case injectModule: TwitterModule =>
        try {
          injectModule.callShutdownCallbacks()
        } catch {
          case e: Throwable =>
            error("Shutdown method error in " + injectModule, e)
        }
      case _ =>
    }
  }
}
