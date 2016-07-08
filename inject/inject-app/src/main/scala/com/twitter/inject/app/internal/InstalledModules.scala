package com.twitter.inject.app.internal

import com.google.inject.util.Modules
import com.google.inject.{Module => GuiceModule, _}
import com.twitter.app.Flag
import com.twitter.inject.{TwitterBaseModule, TwitterModuleLifecycle, Injector, Logging}
import scala.collection.JavaConverters._

private[app] object InstalledModules {

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
   * Recursively capture all flags in the [[com.google.inject.Module]] object hierarchy.
   *
   * Note: We will not (cannot?) traverse through a normal Guice AbstractModule, to find 'installed' [[com.google.inject.Module]]s
   */
  private[app] def findModuleFlags(modules: Seq[GuiceModule]): Seq[Flag[_]] = {
    (modules collect {
      case injectModule: TwitterBaseModule =>
        injectModule.flags ++
          findModuleFlags(injectModule.modules) ++
          findModuleFlags(injectModule.frameworkModules)
    }).flatten.distinct
  }

  /** Recursively finds all 'composed' modules */
  private def findInstalledModules(module: GuiceModule): Seq[GuiceModule] = module match {
    case injectModule: TwitterBaseModule =>
      injectModule.modules ++
        (injectModule.modules flatMap findInstalledModules) ++
        injectModule.frameworkModules ++
        (injectModule.frameworkModules flatMap findInstalledModules)
    case _ =>
      Seq()
  }
}

private[app] case class InstalledModules(
  injector: Injector,
  modules: Seq[GuiceModule])
  extends Logging {

  def postInjectorStartup() {
    modules foreach {
      case injectModule: TwitterModuleLifecycle =>
        try {
          injectModule.singletonStartup(injector)
        } catch {
          case e: Throwable =>
            error("Startup method error in " + injectModule, e)
            throw e
        }
      case _ =>
    }
  }

  def postWarmupComplete(): Unit = {
    modules foreach {
      case injectModule: TwitterModuleLifecycle =>
        try {
          injectModule.singletonPostWarmupComplete(injector)
        } catch {
          case e: Throwable =>
            error("Post warmup complete method error in " + injectModule, e)
            throw e
        }
      case _ =>
    }
  }

  // Note: We don't rethrow so that all modules have a chance to shutdown
  def shutdown() {
    modules foreach {
      case injectModule: TwitterModuleLifecycle =>
        try {
          injectModule.singletonShutdown(injector)
        } catch {
          case e: Throwable =>
            error("Shutdown method error in " + injectModule, e)
        }
      case _ =>
    }
  }
}
