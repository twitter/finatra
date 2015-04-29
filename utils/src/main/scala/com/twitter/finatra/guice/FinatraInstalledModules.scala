package com.twitter.finatra.guice

import com.google.inject.Stage
import com.twitter.app.Flag
import com.twitter.inject.app.internal.InstalledModules

@deprecated("Use com.twitter.inject.app.TestInjector", "")
object FinatraInstalledModules {

  def create(
    flags: Seq[Flag[_]],
    modules: Seq[GuiceModule],
    overrideModules: Seq[GuiceModule],
    stage: Stage = Stage.PRODUCTION): InstalledModules = {

    InstalledModules.create(flags, modules, overrideModules, stage)
  }

}
