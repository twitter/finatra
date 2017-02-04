package com.twitter.inject.app

import com.google.inject.{Module, Stage}
import com.twitter.app.{FlagUsageError, FlagParseException, Flags, Flag}
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

    val flag: Flags = new Flags(
      this.getClass.getSimpleName,
      includeGlobal = true,
      failFastUntilParsed = true)

    val moduleFlags = InstalledModules.findModuleFlags(modules ++ overrideModules)
    moduleFlags.foreach(flag.add)
    parseFlags(flag, flags, moduleFlags)

    InstalledModules.create(
      flags = flag.getAll(includeGlobal = false).toSeq,
      modules = modules,
      overrideModules = overrideModules,
      stage = stage).injector
  }

  /* Private */

  private def parseFlags(flag: Flags, flags: Map[String, String], moduleFlags: Seq[Flag[_]]): Unit = {
    /* Parse all flags with incoming supplied flag values */
    val args = flags.map { case (k, v) => s"-$k=$v" }.toArray

    flag.parseArgs(args) match {
      case Flags.Help(usage) =>
        throw FlagUsageError(usage)
      case Flags.Error(reason) =>
        throw FlagParseException(reason)
      case _ => // nothing
    }
  }
}
