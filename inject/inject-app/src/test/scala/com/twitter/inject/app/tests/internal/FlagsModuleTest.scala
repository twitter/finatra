package com.twitter.inject.app.tests.internal

import com.google.inject.ProvisionException
import com.twitter.app.Flags
import com.twitter.inject.Test
import com.twitter.inject.annotations.Flag
import com.twitter.inject.app.TestInjector
import com.twitter.inject.app.internal.FlagsModule
import javax.inject.Inject

class FlagsModuleTest extends Test {

  val flag = new Flags("FlagsModuleTest", includeGlobal = false, failFastUntilParsed = false)
  val myFlag = flag[String]("my.flag", "This flag has no default")
  val myFlagWithDefault =
    flag("my.flag.with.default", "default value", "this flag has a default value")
  flag.parseArgs(Array())
  val flagsModule = FlagsModule.create(flag.getAll(includeGlobal = false).toSeq)

  test("inject flag with default") {
    val flagValue = TestInjector(flagsModule).create.instance[RequiresFlagWithDefault].flagValue
    assert(flagValue == "default value")
  }

  test("throw exception when injecting flag without default") {
    val e = intercept[ProvisionException] {
      TestInjector(flagsModule).create.instance[RequiresFlag]
    }

    assert(e.getCause.getClass == classOf[IllegalArgumentException])
  }

  test("use flag without default") {
    assert(myFlag.get.isEmpty)
  }
}

class RequiresFlag @Inject()(@Flag("my.flag") val flagValue: String)
class RequiresFlagWithDefault @Inject()(@Flag("my.flag.with.default") val flagValue: String)
