package com.twitter.inject.app.internal

import com.google.inject.util.Modules
import com.google.inject.{Module => GuiceModule, _}
import com.twitter.app.Flag
import com.twitter.inject.{Injector, Logging, TwitterBaseModule, TwitterModuleLifecycle}
import scala.collection.JavaConverters._
import scala.PartialFunction.condOpt

private[app] object InstalledModules {

  /* Public */

  def create(
    flags: Seq[Flag[_]],
    modules: Seq[GuiceModule],
    overrideModules: Seq[GuiceModule],
    stage: Stage = Stage.PRODUCTION
  ): InstalledModules = {

    val allNonOverrideModules = {
      val frameworkModules = Seq(FlagsModule.create(flags), TwitterTypeConvertersModule)

      val composedModules = modules flatMap findInstalledModules
      modules ++ composedModules ++ frameworkModules
    }

    val allOverrideModules = {
      val composedOverrideModules = overrideModules flatMap findInstalledModules
      overrideModules ++ composedOverrideModules
    }

    val combinedModule =
      Modules.`override`(allNonOverrideModules.asJava).`with`(allOverrideModules.asJava)

    /* De-dupe all the modules using a java.util.IdentityHashMap with the modules as keys */
    val identityHashMap = new java.util.IdentityHashMap[GuiceModule, Boolean]()
    (allNonOverrideModules ++ allOverrideModules).foreach { module =>
      if (!identityHashMap.containsKey(module)) identityHashMap.put(module, true)
    }

    new InstalledModules(
      injector = Injector(Guice.createInjector(stage, combinedModule)),
      modules = identityHashMap.keySet().asScala.toSeq
    )
  }

  /* Private */

  /** Recursively capture all flags in the [[com.google.inject.Module]] object hierarchy. */
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

private[app] case class InstalledModules(injector: Injector, modules: Seq[GuiceModule])
    extends Logging {

  def postInjectorStartup(): Unit = {
    modules.foreach {
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
    modules.foreach {
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

  /**
   * Collect shutdown `ExitFunctions` for [[com.google.inject.Module]] instances
   * which implement the [[TwitterModuleLifecycle]]
   */
  def shutdown(): Seq[ExitFunction] = {
    condOptModules(modules)(_.singletonShutdown(injector))
  }

  /**
   * Collect close  `ExitFunctions` for [[com.google.inject.Module]] instances which
   * implement the [[TwitterModuleLifecycle]]
   */
  def close(): Seq[ExitFunction] = {
    condOptModules(modules)(_.close())
  }

  /* Private */

  /**
   * Iterates through the list of Modules to match only instances of TwitterModuleLifecycle
   * on which to create an `ExitFunction` over the passed in TwitterModuleLifecycle function.
   * @see [[scala.PartialFunction.condOpt]]
   */
  private[this] def condOptModules(
    modules: Seq[GuiceModule]
  )(
    fn: (TwitterModuleLifecycle) => Unit
  ): Seq[ExitFunction] = modules.flatMap { module =>
    condOpt(module) {
      case injectModule: TwitterModuleLifecycle =>
        () =>
          fn(injectModule)
    }
  }
}
