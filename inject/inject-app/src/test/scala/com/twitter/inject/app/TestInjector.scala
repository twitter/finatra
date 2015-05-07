package com.twitter.inject.app

import com.google.inject.{Module, Stage}
import com.twitter.app.Flag
import com.twitter.inject.app.internal.InstalledModules
import com.twitter.inject.{Injector, TwitterModule}

object TestInjector {

  /* Public */

  def apply(modules: Module*): Injector = {
    apply(modules = modules)
  }

  def apply(
    clientFlags: Map[String, String] = Map(),
    modules: Seq[Module],
    overrideModules: Seq[Module] = Seq(),
    stage: Stage = Stage.DEVELOPMENT): Injector = {

    val moduleFlags = InstalledModules.findModuleFlags(modules ++ overrideModules)

    parseClientFlags(
      clientFlags,
      moduleFlags)

    InstalledModules.create(
      flags = moduleFlags,
      modules = modules,
      overrideModules = overrideModules,
      stage = stage).injector
  }

  /* Private */

  /*
   * First we try to parse module flags with client provided flags. If a
   * module flag isn't found, we set a system property which allows us to
   * set GlobalFlags (e.g. resolverMap) that aren't found in modules.
   * Note: We originally tried classpath scanning for the GlobalFlags using the Flags class,
   * but this added many seconds to each test and also regularly ran out of perm gen...
   */
  private def parseClientFlags(clientFlags: Map[String, String], moduleFlags: Seq[Flag[_]]) {
    val moduleFlagsMap = moduleFlags groupBy {_.name} mapValues {_.head}

    /* Parse module flags with client supplied flag values */
    for (moduleFlag <- moduleFlags) {
      clientFlags.get(moduleFlag.name) match {
        case Some(clientFlagValue) => moduleFlag.parse(clientFlagValue)
        case _ => moduleFlag.parse()
      }
    }

    /* Set system property for clientFlags not found in moduleFlags */
    for {
      (clientFlagName, clientFlagValue) <- clientFlags
      if !moduleFlagsMap.contains(clientFlagName)
    } {
      System.setProperty(clientFlagName, clientFlagValue)
    }
  }
}
