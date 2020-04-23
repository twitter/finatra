package com.twitter.inject.app.tests

import com.google.inject.{ConfigurationException, Guice, ProvisionException}
import com.twitter.app.{Flaggable, Flags}
import com.twitter.conversions.StorageUnitOps._
import com.twitter.inject.annotations.{Flag, Flags => AnnotationFlags}
import com.twitter.inject.app.internal.{FlagsModule, TwitterTypeConvertersModule}
import com.twitter.inject.{Injector, Test}
import com.twitter.util.{Duration, StorageUnit, Time, TimeFormat}
import java.net.InetSocketAddress
import java.time.LocalTime
import javax.inject.Inject

class FlagsModuleTest extends Test {
  // we should consider adding this Flaggable to c.t.app.Flaggable
  private implicit val ofJavaLocalTime: Flaggable[LocalTime] = new Flaggable[LocalTime] {
    override def parse(s: String): LocalTime = LocalTime.parse(s)
  }

  private[this] val flag =
    new Flags(this.getClass.getName, includeGlobal = false, failFastUntilParsed = false)

  private[this] val myFlag = flag[String]("my.flag", "This flag has no default")
  private[this] val myFlagWithDefault =
    flag("my.flag.with.default", "default value", "This flag has a default value")
  private[this] val myFlagWithValue = flag(
    "my.flag.value",
    "foo",
    "This flag has a default but it will be overridden by a parsed value"
  )

  private[this] val defaultTimeFormat = new TimeFormat("yyyy-MM-dd HH:mm:ss Z")
  private[this] val defaultLocalTime = LocalTime.MIN
  private[this] val defaultTime = defaultTimeFormat.parse("2020-04-21 00:00:00 -0700")
  private[this] val defaultDuration = Duration.fromMilliseconds(100)
  private[this] val defaultStorageUnit = 2.gigabytes
  private[this] val defaultInetSocketAddress: InetSocketAddress =
    InetSocketAddress.createUnresolved("localhost", 0)
  flag[LocalTime]("local.time", defaultLocalTime, "A java.time.LocalTime")
  flag[Time]("twitter.time", defaultTime, "A twitter util Time")
  flag[Duration]("twitter.duration", defaultDuration, "A twitter util Duration")
  flag[StorageUnit]("storage.unit", defaultStorageUnit, "Represents a storage size")
  flag[InetSocketAddress]("some.address", defaultInetSocketAddress, "An InetSocketAddress")

  flag.parseArgs(Array("-my.flag.value=bar"))

  private[this] val flagsModule = new FlagsModule(flag)
  // Users should prefer the `c.t.inject.app.TestInjector`. Here we are testing the FlagsModule
  // with a collection of already parsed Flags. The FlagsModule is used in creation of the
  // underlying injector inside the TestInjector, thus we need to manually create an Injector.
  private[this] val injector: Injector = Injector(Guice.createInjector(flagsModule, TwitterTypeConvertersModule))

  test("inject flag with default") {
    injector.instance[RequiresFlagWithDefault].flagValue should equal("default value")
    injector.instance[String](AnnotationFlags.named("my.flag.with.default")) should equal(
      "default value"
    )
  }

  test("throw exception when injecting flag without default") {
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

  test("flags are bound") {
    val flgs = injector.instance[com.twitter.inject.Flags]
    flag.getAll(includeGlobal = false).toSeq.sortBy(_.name) should equal(flgs.getAll.sortBy(_.name))
  }

  test("test type converters") {
    injector.instance[LocalTime](AnnotationFlags.named("local.time")) should equal(defaultLocalTime)
    injector.instance[Time](AnnotationFlags.named("twitter.time")) should equal(defaultTime)
    injector.instance[Duration](AnnotationFlags.named("twitter.duration")) should equal(
      defaultDuration)
    injector.instance[StorageUnit](AnnotationFlags.named("storage.unit")) should equal(
      defaultStorageUnit)
    val inetSocketAddressFromInjector =
      injector.instance[InetSocketAddress](AnnotationFlags.named("some.address"))
    inetSocketAddressFromInjector.getHostName should equal(defaultInetSocketAddress.getHostName)
    inetSocketAddressFromInjector.getPort should equal(defaultInetSocketAddress.getPort)
  }

  test("injector without TwitterTypeConvertersModule module fails to inject flag by type") {
    val injctr = Injector(Guice.createInjector(flagsModule))
    intercept[ConfigurationException] {
      injctr.instance[LocalTime](AnnotationFlags.named("local.time"))
    }
    intercept[ConfigurationException] {
      injctr.instance[Time](AnnotationFlags.named("twitter.time"))
    }
    intercept[ConfigurationException] {
      injctr.instance[Duration](AnnotationFlags.named("twitter.duration"))
    }
    intercept[ConfigurationException] {
      injctr.instance[StorageUnit](AnnotationFlags.named("storage.unit"))
    }
    intercept[ConfigurationException] {
      injctr.instance[InetSocketAddress](AnnotationFlags.named("some.address"))
    }

    // can be asked for as Strings
    injector.instance[String](AnnotationFlags.named("local.time")) should equal(defaultLocalTime.toString)
    injector.instance[String](AnnotationFlags.named("twitter.time")) should equal(defaultTime.toString)
    injector.instance[String](AnnotationFlags.named("twitter.duration")) should equal(defaultDuration.toString)
    injector.instance[String](AnnotationFlags.named("storage.unit")) should equal(defaultStorageUnit.toString())
    injector.instance[String](AnnotationFlags.named("some.address")) should equal(defaultInetSocketAddress.toString)
  }
}

class RequiresFlag @Inject() (@Flag("my.flag") val flagValue: String)
class RequiresFlagWithDefault @Inject() (@Flag("my.flag.with.default") val flagValue: String)
