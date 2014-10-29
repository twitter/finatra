package com.twitter.finatra.guice

import com.google.inject.Stage
import com.twitter.app.Flag
import com.twitter.finatra.conversions.seq._

object FinatraTestInjector {

  /* Public */

  def apply(modules: GuiceModule*): FinatraInjector = {
    apply(modules = modules)
  }

  def apply(
    clientFlags: Map[String, String] = Map(),
    modules: Seq[GuiceModule],
    overrideModules: Seq[GuiceModule] = Seq(),
    stage: Stage = Stage.DEVELOPMENT): FinatraInjector = {

    val moduleFlags = FinatraInstalledModules.findModuleFlags(modules ++ overrideModules)

    parseClientFlags(
      clientFlags,
      moduleFlags)

    FinatraInstalledModules.create(
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
    val moduleFlagsMap = moduleFlags groupBySingleValue {_.name}

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
