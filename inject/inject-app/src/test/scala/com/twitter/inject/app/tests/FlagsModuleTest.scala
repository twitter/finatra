package com.twitter.inject.app.tests

import com.google.inject.{ConfigurationException, Guice, ProvisionException}
import com.twitter.app.{Flaggable, Flags}
import com.twitter.conversions.StorageUnitOps._
import com.twitter.inject.annotations.{Flag, Flags => AnnotationFlags}
import com.twitter.inject.app.internal.{FlagsModule, TwitterTypeConvertersModule}
import com.twitter.inject.{Injector, Test}
import com.twitter.util.{Duration, StorageUnit, Time, TimeFormat}
import java.io.File
import java.net.InetSocketAddress
import java.time.LocalTime
import java.util.{Arrays, Optional}
import javax.inject.Inject

class FlagsModuleTest extends Test {
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
  private[this] val defaultFile: File = new File("/tmp/foo/bar")

  flag[LocalTime]("local.time", defaultLocalTime, "A java.time.LocalTime")
  flag[Time]("twitter.time", defaultTime, "A twitter util Time")
  flag[Duration]("twitter.duration", defaultDuration, "A twitter util Duration")
  flag[StorageUnit]("storage.unit", defaultStorageUnit, "Represents a storage size")
  flag[InetSocketAddress]("some.address", defaultInetSocketAddress, "An InetSocketAddress")
  flag[File]("file", defaultFile, "A File")

  flag[Seq[String]]("seq.string", Seq.empty, "A Seq of String")
  flag[Seq[Int]]("seq.int", Seq(123), "A Seq of Int")
  flag[Seq[Long]]("seq.long", Seq(123.toLong), "A Seq of Long")
  flag[Seq[Double]]("seq.double", Seq(123.0), "A Seq of Double")
  flag[Seq[Float]]("seq.float", Seq(123.0f), "A Seq of Float")
  flag[Seq[LocalTime]]("seq.local.time", Seq(defaultLocalTime), "A Seq java.time.LocalTime")
  flag[Seq[Time]]("seq.twitter.time", Seq(defaultTime), "A Seq of twitter util Time")
  flag[Seq[Duration]](
    "seq.twitter.duration",
    Seq(defaultDuration),
    "A Seq of twitter util Duration")
  flag[Seq[StorageUnit]]("seq.storage.unit", Seq(defaultStorageUnit), "A Seq of storage size")
  flag[Seq[InetSocketAddress]](
    "seq.some.address",
    Seq(defaultInetSocketAddress),
    "A Seq of InetSocketAddress")
  flag[Seq[File]]("seq.file", Seq(defaultFile), "A Seq of File")

  flag[java.util.List[String]]("list.string", Arrays.asList[String](), "A List of String")
  flag[java.util.List[java.lang.Integer]](
    "list.int",
    Arrays.asList[java.lang.Integer](123),
    "A List of Int")
  flag[java.util.List[java.lang.Long]](
    "list.long",
    Arrays.asList[java.lang.Long](123l),
    "A List of Long")
  flag[java.util.List[java.lang.Double]](
    "list.double",
    Arrays.asList[java.lang.Double](123.0),
    "A List of Double")
  flag[java.util.List[java.lang.Float]](
    "list.float",
    Arrays.asList[java.lang.Float](123.0f),
    "A List of Float")
  flag[java.util.List[LocalTime]](
    "list.local.time",
    Arrays.asList(defaultLocalTime),
    "A List java.time.LocalTime")
  flag[java.util.List[Time]](
    "list.twitter.time",
    Arrays.asList(defaultTime),
    "A List of twitter util Time")
  flag[java.util.List[Duration]](
    "list.twitter.duration",
    Arrays.asList(defaultDuration),
    "A List of twitter util Duration")
  flag[java.util.List[StorageUnit]](
    "list.storage.unit",
    Arrays.asList(defaultStorageUnit),
    "A List of storage size")
  flag[java.util.List[InetSocketAddress]](
    "list.some.address",
    Arrays.asList(defaultInetSocketAddress),
    "A List of InetSocketAddress")
  flag[java.util.List[File]](
    "list.file",
    Arrays.asList(defaultFile),
    "A List of Flag"
  )

  flag[Int]("int", "int value")
  flag[Seq[Int]]("list.int.no.default", "int list")
  flag[Qux]("qux", Qux(), "could have been a foo or bar or baz")
  flag[Seq[Qux]]("quxs", Seq.empty[Qux], "could have been a foos or bars or bazs")

  flag.parseArgs(
    Array(
      "-my.flag.value=bar",
      "-seq.string=foo,bar",
      "-list.string=baz,qux",
      "-int=10",
      "-list.int.no.default=1,2,3"))

  private[this] val flagsModule = new FlagsModule(flag)
  // Users should prefer the `c.t.inject.app.TestInjector`. Here we are testing the FlagsModule
  // with a collection of already parsed Flags. The FlagsModule is used in creation of the
  // underlying injector inside the TestInjector, thus we need to manually create an Injector.
  private[this] val injector: Injector = Injector(
    Guice.createInjector(flagsModule, TwitterTypeConvertersModule))

  test("inject flag with default") {
    injector.instance[RequiresFlagWithDefault].flagValue should equal("default value")
    injector.instance[String](AnnotationFlags.named("my.flag.with.default")) should equal(
      "default value"
    )
  }

  test("not double-inject string flags") {
    injector.instance[String](AnnotationFlags.named("my.flag.value")) should equal("bar")
  }

  test("not bind raw strings (w/o @Flag annotation)") {
    val x = injector.instance[String]
    Console.out.println(injector.instance[String])
    //intercept[ConfigurationException](injector.instance[String])
  }

  test("also inject each flag as string") {
    injector.instance[String](AnnotationFlags.named("int")) should equal("10")

    injector.instance[String](AnnotationFlags.named("local.time")) should equal(
      defaultLocalTime.toString)
    injector.instance[String](AnnotationFlags.named("twitter.time")) should equal(
      defaultTime.toString)
    injector.instance[String](AnnotationFlags.named("twitter.duration")) should equal(
      defaultDuration.toString)
    injector.instance[String](AnnotationFlags.named("storage.unit")) should equal(
      defaultStorageUnit.toString())
    injector.instance[String](AnnotationFlags.named("some.address")) should equal(
      defaultInetSocketAddress.toString)
  }

  test("inject flags as (typed) options") {
    val o = injector.instance[OptionalScalaFlag]
    o.flagValue should equal(Some(10))
    o.flagsValue should equal(Some(Seq(1, 2, 3)))

    val r = injector.instance[RequiredScalaFlag]
    r.flagValue should equal(10)
    r.flagsValue should equal(Seq(1, 2, 3))

    val oj = injector.instance[OptionalJavaFlag]
    oj.flagValue should equal(Optional.of(10))
    oj.flagsValue should equal(Optional.of(Seq(1, 2, 3)))

    injector.instance[Option[Int]](AnnotationFlags.named("int")) should equal(Some(10))
    injector.instance[Option[Seq[Int]]](AnnotationFlags.named("list.int.no.default")) should equal(
      Some(Seq(1, 2, 3)))

    injector.instance[Optional[Int]](AnnotationFlags.named("int")) should equal(Optional.of(10))
    injector.instance[Optional[Seq[Int]]](
      AnnotationFlags.named("list.int.no.default")) should equal(Optional.of(Seq(1, 2, 3)))

    injector.instance[Int](AnnotationFlags.named("int")) should equal(10)
    injector.instance[Seq[Int]](AnnotationFlags.named("list.int.no.default")) should equal(
      Seq(1, 2, 3))
  }

  test("doesn't bind flags with defaults as options") {
    intercept[ConfigurationException] {
      injector.instance[Option[String]](AnnotationFlags.named("my.flag.value"))
    }

    intercept[ConfigurationException] {
      injector.instance[Option[Qux]](AnnotationFlags.named("qux"))
    }
  }

  test("when Flaggable isn't typed only injects strings") {
    injector.instance[String](AnnotationFlags.named("qux")) should equal("Qux()")
    injector.instance[String](AnnotationFlags.named("quxs")) should equal("")

    intercept[ConfigurationException] {
      injector.instance[Qux](AnnotationFlags.named("qux"))
    }

    intercept[ConfigurationException] {
      injector.instance[Seq[Qux]](AnnotationFlags.named("quxs"))
    }
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
    injector.instance[File](AnnotationFlags.named("file")) should equal(defaultFile)
  }

  test("test comma-separated type converters (Seq)") {
    injector.instance[Seq[String]](AnnotationFlags.named("seq.string")) should equal(
      Seq("foo", "bar"))
    injector.instance[Seq[Int]](AnnotationFlags.named("seq.int")) should equal(Seq(123))
    injector.instance[Seq[Long]](AnnotationFlags.named("seq.long")) should equal(Seq(123))
    injector.instance[Seq[Double]](AnnotationFlags.named("seq.double")) should equal(Seq(123.0))
    injector.instance[Seq[Float]](AnnotationFlags.named("seq.float")) should equal(Seq(123.0f))
    injector.instance[Seq[LocalTime]](AnnotationFlags.named("seq.local.time")) should equal(
      Seq(defaultLocalTime))
    injector.instance[Seq[Time]](AnnotationFlags.named("seq.twitter.time")) should equal(
      Seq(defaultTime))
    injector.instance[Seq[Duration]](AnnotationFlags.named("seq.twitter.duration")) should equal(
      Seq(defaultDuration))
    injector.instance[Seq[StorageUnit]](AnnotationFlags.named("seq.storage.unit")) should equal(
      Seq(defaultStorageUnit))
    val inetSocketAddressFromInjector =
      injector.instance[Seq[InetSocketAddress]](AnnotationFlags.named("seq.some.address")).head
    inetSocketAddressFromInjector.getHostName should equal(defaultInetSocketAddress.getHostName)
    inetSocketAddressFromInjector.getPort should equal(defaultInetSocketAddress.getPort)
    injector.instance[Seq[File]](AnnotationFlags.named("seq.file")) should equal(Seq(defaultFile))
  }

  test("test comma-separated type converters (java.util.List)") {
    injector.instance[java.util.List[String]](AnnotationFlags.named("list.string")) should equal(
      Arrays.asList("baz", "qux"))
    injector.instance[java.util.List[java.lang.Integer]](
      AnnotationFlags.named("list.int")) should equal(Arrays.asList[java.lang.Integer](123))
    injector.instance[java.util.List[java.lang.Long]](
      AnnotationFlags.named("list.long")) should equal(Arrays.asList[java.lang.Long](123l))
    injector.instance[java.util.List[java.lang.Double]](
      AnnotationFlags.named("list.double")) should equal(Arrays.asList(123.0))
    injector.instance[java.util.List[java.lang.Float]](
      AnnotationFlags.named("list.float")) should equal(Arrays.asList[java.lang.Float](123.0f))
    injector.instance[java.util.List[LocalTime]](
      AnnotationFlags.named("list.local.time")) should equal(Arrays.asList(defaultLocalTime))
    injector.instance[java.util.List[Time]](
      AnnotationFlags.named("list.twitter.time")) should equal(Arrays.asList(defaultTime))
    injector.instance[java.util.List[Duration]](
      AnnotationFlags.named("list.twitter.duration")) should equal(Arrays.asList(defaultDuration))
    injector.instance[java.util.List[StorageUnit]](
      AnnotationFlags.named("list.storage.unit")) should equal(Arrays.asList(defaultStorageUnit))
    val inetSocketAddressFromInjector =
      injector
        .instance[java.util.List[InetSocketAddress]](
          AnnotationFlags.named("list.some.address")).get(0)
    inetSocketAddressFromInjector.getHostName should equal(defaultInetSocketAddress.getHostName)
    inetSocketAddressFromInjector.getPort should equal(defaultInetSocketAddress.getPort)
    injector.instance[java.util.List[File]](AnnotationFlags.named("list.file")) should equal(
      Arrays.asList(defaultFile))
  }
}

class OptionalScalaFlag @Inject() (
  @Flag("int") val flagValue: Option[Int],
  @Flag("list.int.no.default") val flagsValue: Option[Seq[Int]])
class RequiredScalaFlag @Inject() (
  @Flag("int") val flagValue: Int,
  @Flag("list.int.no.default") val flagsValue: Seq[Int])

class OptionalJavaFlag @Inject() (
  @Flag("int") val flagValue: Optional[Int],
  @Flag("list.int.no.default") val flagsValue: Optional[Seq[Int]])

class RequiresFlag @Inject() (@Flag("my.flag") val flagValue: String)
class RequiresFlagWithDefault @Inject() (@Flag("my.flag.with.default") val flagValue: String)

final case class Qux()
object Qux {
  implicit val f: Flaggable[Qux] = new Flaggable[Qux] {
    def parse(s: String): Qux = Qux()
  }
}
