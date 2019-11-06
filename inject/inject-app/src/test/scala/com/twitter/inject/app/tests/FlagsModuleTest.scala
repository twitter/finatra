package com.twitter.inject.app.tests

import com.google.inject.ProvisionException
import com.twitter.app.Flags
import com.twitter.inject.Test
import com.twitter.inject.annotations.{Flag, Flags => AnnotationFlags}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.app.internal.FlagsModule
import javax.inject.Inject

class FlagsModuleTest extends Test {
  private[this] val flag =
    new Flags("FlagsModuleTest", includeGlobal = false, failFastUntilParsed = false)

  private[this] val myFlag = flag[String]("my.flag", "This flag has no default")
  private[this] val myFlagWithDefault =
    flag("my.flag.with.default", "default value", "This flag has a default value")
  private[this] val myFlagWithValue = flag(
    "my.flag.value",
    "foo",
    "This flag has a default but it will be overridden by a parsed value"
  )

  flag.parseArgs(Array("-my.flag.value=bar"))

  private[this] val flagsModule = FlagsModule.create(flag.getAll(includeGlobal = false).toSeq)

  test("inject flag with default") {
    val injector = TestInjector(flagsModule).create
    injector.instance[RequiresFlagWithDefault].flagValue should equal("default value")
    injector.instance[String](AnnotationFlags.named("my.flag.with.default")) should equal(
      "default value"
    )
  }

  test("throw exception when injecting flag without default") {
    val injector = TestInjector(flagsModule).create

    // flags without a default cannot be used in injection
    val e1 = intercept[ProvisionException] {
      injector.instance[RequiresFlag]
    }
    e1.getCause.getClass should equal(classOf[IllegalArgumentException])

    val e2 = intercept[ProvisionException] {
      injector.instance[String](AnnotationFlags.named("my.flag"))
    }
    e2.getCause.getClass should equal(classOf[IllegalArgumentException])
  }

  test("use flag without default") {
    myFlag.get.isEmpty should be(true)

    val t = intercept[IllegalArgumentException] {
      myFlag()
    }
    // a flag with no default and not given a value when parsed is effectively "not found"
    t.getMessage.contains("not found") should be(true)
  }

  test("use flag with default") {
    myFlagWithDefault() should equal("default value")
  }

  test("use flag with value") {
    // default value is "foo", parsed value should be "bar"
    myFlagWithValue() should equal("bar")
  }
}

class RequiresFlag @Inject()(@Flag("my.flag") val flagValue: String)
class RequiresFlagWithDefault @Inject()(@Flag("my.flag.with.default") val flagValue: String)
