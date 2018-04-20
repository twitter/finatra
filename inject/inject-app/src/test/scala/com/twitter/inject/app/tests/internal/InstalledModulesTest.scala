package com.twitter.inject.app.tests.internal

import com.twitter.inject.Test
import com.twitter.inject.app.internal.InstalledModules

class InstalledModulesTest extends Test {

  test("Modules are de-duped") {
    val counter = 0

    val module1 = new TestModuleClass("instance1", counter)

    val modules = Seq(
      module1,
      TestModuleObject) ++ Seq(
      new TestModuleClass("instance2", counter),
      TestModuleObject) ++ Seq(
      module1)

    modules.size should be(5)

    val installedModules = InstalledModules.create(
      flags = Seq.empty,
      modules = modules,
      overrideModules = Seq.empty)

    // there are only 3 distinct modules, TestModuleObject, TestModuleClass(instance1), TestModuleClass(instance2)
    installedModules.modules.size should be(5) // the framework adds two modules: FlagsModule and TwitterTypeConvertersModule
    installedModules.postInjectorStartup()

    module1.counter should be(1)

    // we should have only run the update of the StateMap counter once in the single TestModuleObject
    val stateMap = installedModules.injector.instance[StateMap]
    stateMap.internals.size should be(1)
    stateMap.internals("key") should be(1)
  }
}
