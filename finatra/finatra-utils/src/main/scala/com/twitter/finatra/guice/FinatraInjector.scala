package com.twitter.finatra.guice

import com.google.inject.Stage.PRODUCTION
import com.google.inject.name.Names
import com.google.inject.util.Modules
import com.google.inject.{Injector, Guice, Key, Module}
import com.twitter.app.Flag
import com.twitter.finatra.utils.Logging
import java.lang.annotation.{Annotation => JavaAnnotation}
import net.codingwell.scalaguice.KeyExtensions._
import net.codingwell.scalaguice._
import scala.PartialFunction._
import scala.collection.JavaConverters._


object FinatraInjector {

  /* Public */

  def apply(
    flags: Seq[Flag[_]],
    modules: Seq[Module],
    overrideModules: Seq[Module]): FinatraInjector = {

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

    new FinatraInjector(
      guiceInjector = Guice.createInjector(PRODUCTION, combinedModule),
      allModules =
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

case class FinatraInjector(
  guiceInjector: Injector,
  allModules: Seq[Module] = Seq())
  extends Logging {

  /* Public */

  def instance[T: Manifest]: T = guiceInjector.getInstance(typeLiteral[T].toKey)

  def instance[T: Manifest, Ann <: JavaAnnotation : Manifest]: T = {
    val annotation = manifest[Ann].erasure.asInstanceOf[Class[Ann]]
    instance[T](annotation)
  }

  def instance[T: Manifest](name: String): T = {
    instance[T](Names.named(name))
  }

  def instance[T](clazz: Class[T]): T = guiceInjector.getInstance(clazz)

  def instance[T](key: Key[T]): T = guiceInjector.getInstance(key)

  def postStartup() {
    allModules foreach postStartup
  }

  def postWarmup() {
    allModules foreach postWarmup
  }

  def shutdown() {
    allModules foreach shutdown
  }

  /* Private */

  private def postStartup(module: Module) = condOpt(module) {
    case guiceModule: GuiceModule =>
      try {
        guiceModule.callPostStartupCallbacks()
      } catch {
        case e: Throwable =>
          error("Startup method error in " + module, e)
          throw e
      }
  }

  private def postWarmup(module: Module) = condOpt(module) {
    case guiceModule: GuiceModule =>
      try {
        guiceModule.callPostWarmupCallbacks()
      } catch {
        case e: Throwable =>
          error("PostWarmup method error in " + module, e)
          throw e
      }
  }

  // Note: We don't rethrow so that all modules have a change to shutdown
  private def shutdown(module: Module) = condOpt(module) {
    case guiceModule: GuiceModule =>
      try {
        guiceModule.callShutdownCallbacks()
      } catch {
        case e: Throwable =>
          error("Shutdown method error in " + module, e)
      }
  }

  private def instance[T: Manifest](annotationType: Class[_ <: JavaAnnotation]): T = {
    val key = Key.get(typeLiteral[T], annotationType)
    guiceInjector.getInstance(key)
  }

  private def instance[T: Manifest](annotation: JavaAnnotation): T = {
    val key = Key.get(typeLiteral[T], annotation)
    guiceInjector.getInstance(key)
  }
}
