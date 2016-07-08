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
  val myFlagWithDefault = flag("my.flag.with.default", "default value", "this flag has a default value")
  flag.parseArgs(Array())
  val flagsModule = FlagsModule.create(flag.getAll(includeGlobal = false).toSeq)

  "FlagsModule" should {
    "inject flag with default" in {
      TestInjector(flagsModule).instance[RequiresFlagWithDefault].flagValue should be("default value")
    }

    "throw exception when injecting flag without default" in {
      intercept[ProvisionException] {
        TestInjector(flagsModule).instance[RequiresFlag]
      }.getCause shouldBe an[IllegalArgumentException]
    }

    "use flag without default" in {
      myFlag.get should be(None)
    }
  }
}

class RequiresFlag @Inject()(@Flag("my.flag") val flagValue: String)
class RequiresFlagWithDefault @Inject()(@Flag("my.flag.with.default") val flagValue: String)