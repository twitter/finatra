package com.twitter.inject.app

import com.google.inject.{Module, Stage}
import com.twitter.app.Flag
import com.twitter.inject.Injector
import com.twitter.inject.app.internal.InstalledModules

object TestInjector {

  /* Public */

  def apply(modules: Module*): Injector = {
    apply(modules = modules)
  }

  def apply(
    flags: Map[String, String] = Map(),
    modules: Seq[Module],
    overrideModules: Seq[Module] = Seq(),
    stage: Stage = Stage.DEVELOPMENT): Injector = {

    val moduleFlags = InstalledModules.findModuleFlags(modules ++ overrideModules)

    parseFlags(
      flags,
      moduleFlags)

    InstalledModules.create(
      flags = moduleFlags,
      modules = modules,
      overrideModules = overrideModules,
      stage = stage).injector
  }

  /* Private */

  /*
   * First we try to parse module flags with the provided set flags. If a
   * module flag isn't found, we set a system property which allows us to
   * set GlobalFlags (e.g. resolverMap) that aren't found in modules.
   * Note: We originally tried classpath scanning for the GlobalFlags using the Flags class,
   * but this added many seconds to each test and also regularly ran out of perm gen...
   */
  private def parseFlags(flags: Map[String, String], moduleFlags: Seq[Flag[_]]) {
    val moduleFlagsMap = moduleFlags groupBy {_.name} mapValues {_.head}

    /* Parse module flags with incoming supplied flag values */
    for (moduleFlag <- moduleFlags) {
      flags.get(moduleFlag.name) match {
        case Some(setFlagValue) => moduleFlag.parse(setFlagValue)
        case _ => moduleFlag.parse()
      }
    }

    /* Set system property for flags not found in moduleFlags */
    for {
      (setFlagName, setFlagValue) <- flags
      if !moduleFlagsMap.contains(setFlagName)
    } {
      System.setProperty(setFlagName, setFlagValue)
    }
  }
}
