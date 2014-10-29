package com.twitter.finatra.guice

import com.google.inject._
import com.google.inject.util.Modules
import com.twitter.app.Flag
import com.twitter.finatra.utils.Logging
import scala.collection.JavaConverters._
import com.twitter.finatra.conversions.seq._


object FinatraInstalledModules {

  /* Public */

  def create(
    flags: Seq[Flag[_]],
    modules: Seq[Module],
    overrideModules: Seq[Module],
    stage: Stage = Stage.PRODUCTION): FinatraInstalledModules = {

    val allNonOverrideModules = {
      val frameworkModules = Seq(
        GuiceFlagsModule.create(flags),
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

    new FinatraInstalledModules(
      injector = FinatraInjector(Guice.createInjector(stage, combinedModule)),
      modules =
        allNonOverrideModules ++ allOverrideModules)
  }

  /* Private */

  /**
   * Recursively capture all flags in the GuiceModule object hierarchy.
   *
   * Note: We will not (cannot?) traverse through a normal Guice AbstractModule, to find 'installed' GuiceModules
   */
  private[finatra] def findModuleFlags(modules: Seq[Module]): Seq[Flag[_]] = {
    (modules collect {
      case guiceModule: GuiceModule =>
        guiceModule.flags ++ findModuleFlags(guiceModule.modules)
    }).flatten.distinct
  }

  /** Recursively finds all 'composed' modules */
  private def findInstalledModules(module: Module): Seq[Module] = module match {
    case guiceModule: GuiceModule =>
      guiceModule.modules ++
        (guiceModule.modules flatMap findInstalledModules)
    case _ =>
      Seq()
  }
}

case class FinatraInstalledModules(
  injector: FinatraInjector,
  modules: Seq[Module])
  extends Logging {
  
  def postStartup() {
    modules foreachPartial {
      case guiceModule: GuiceModule =>
        try {
          guiceModule.callPostStartupCallbacks()
        } catch {
          case e: Throwable =>
            error("Startup method error in " + guiceModule, e)
            throw e
        }
    }
  }

  def postWarmup() {
    modules foreachPartial {
      case guiceModule: GuiceModule =>
        try {
          guiceModule.callPostWarmupCallbacks()
        } catch {
          case e: Throwable =>
            error("PostWarmup method error in " + guiceModule, e)
            throw e
        }
    }
  }

  // Note: We don't rethrow so that all modules have a change to shutdown
  def shutdown() {
    modules foreachPartial {
      case guiceModule: GuiceModule =>
        try {
          guiceModule.callShutdownCallbacks()
        } catch {
          case e: Throwable =>
            error("Shutdown method error in " + guiceModule, e)
        }
    }
  }
}
