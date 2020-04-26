package com.twitter.inject.app.tests

import com.google.inject.Stage
import com.twitter.app.Flags
import com.twitter.inject.Test
import com.twitter.inject.app.internal.Modules

class ModulesTest extends Test {

  test("Modules are distinct") {
    val counter = 0

    val module1 = new TestModuleClass("instance1", counter)

    val modules = Seq(
      module1,
      TestModuleObject) ++ Seq(
      new TestModuleClass("instance2", counter),
      TestModuleObject) ++ Seq(
      module1)

    modules.size should be(5)

    val installedModules = new Modules(
      modules,
      Seq.empty
    ).install(
      flags = new Flags(this.getClass.getName),
      stage = Stage.PRODUCTION)

    // there are only 3 distinct modules, TestModuleObject, TestModuleClass(instance1), TestModuleClass(instance2)
    installedModules.modules.size should be(5) // the framework adds two modules: FlagsModule and TwitterTypeConvertersModule
    installedModules.postInjectorStartup()

    module1.counter should be(1)

    // we should have only run the update of the StateMap counter once in the single TestModuleObject
    val stateMap = installedModules.injector.instance[StateMap]
    stateMap.internals.size should be(1)
    stateMap.internals("key") should be(1)
  }

  test("Modules are distinct in a stable order") {
    val modules = (0 until 10).map { i =>
      new TestModuleClass(s"instance$i", 0)
    }

    val deduped = Modules.distinctModules(
      modules.zip(modules).flatMap { case (one, two) => Seq(one, two) })
    deduped should equal(modules)
  }
}
