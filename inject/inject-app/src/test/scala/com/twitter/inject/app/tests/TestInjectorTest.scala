package com.twitter.inject.app.tests

import com.twitter.app.GlobalFlag
import com.twitter.finatra.tests.Prod
import com.twitter.inject.annotations.Flag
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Test, TwitterModule}
import javax.inject.Inject

object testBooleanGlobalFlag extends GlobalFlag[Boolean](false, "Test boolean global flag defaulted to false")
object testStringGlobalFlag extends GlobalFlag[String]("foo", "Test string global flag defaulted to foo")
object testMapGlobalFlag extends GlobalFlag[Map[String, String]](Map.empty, "Test map global flag defaulted to Map.empty")

object BooleanFlagModule extends TwitterModule {
  flag[Boolean]("x", false, "default to false")
}

object TestBindModule extends TwitterModule {
  override protected def configure(): Unit = {
    bind[String, Prod].toInstance("Hello, world!")
    bind[Baz].toInstance(new Baz(10))
  }
}

class Foo @Inject()(@Flag("x") x: Boolean) {
  def bar = x
}

class Bar {
  // read global flags
  val booleanGlobalFlag = testBooleanGlobalFlag()
  val stringGlobalFlag = testStringGlobalFlag()
  val mapGlobalFlag = testMapGlobalFlag()
}

class Baz(val value: Int)

class TestInjectorTest extends Test {

  override protected def afterEach(): Unit = {
    // reset flags
    testBooleanGlobalFlag.reset()
    testStringGlobalFlag.reset()
    testMapGlobalFlag.reset()

    super.afterEach()
  }

  test("default boolean flags properly") {
    val injector = TestInjector(BooleanFlagModule).create
    val foo = injector.instance[Foo]
    assert(!foo.bar)
  }

  test("default global flags properly") {
    val injector = TestInjector().create
    val bar = injector.instance[Bar]
    assert(!bar.booleanGlobalFlag)
    assert(bar.stringGlobalFlag == "foo")
    assert(bar.mapGlobalFlag == Map.empty)
  }

  test("set global flags values properly") {
    val injector = TestInjector(
      modules = Seq(),
      flags = Map(
        "com.twitter.inject.app.tests.testBooleanGlobalFlag" -> "true",
        "com.twitter.inject.app.tests.testStringGlobalFlag" -> "bar",
        "com.twitter.inject.app.tests.testMapGlobalFlag" -> "key1=foo,key2=bar"))
      .create
    val bar = injector.instance[Bar]
    assert(bar.booleanGlobalFlag)
    assert(bar.stringGlobalFlag == "bar")
    assert(bar.mapGlobalFlag == Map("key1" -> "foo", "key2" -> "bar"))
  }

  test("module defaults") {
    val injector = TestInjector(
      modules = Seq(TestBindModule))
      .create

    assert(injector.instance[Baz].value == 10)
    assert(injector.instance[String, Prod] == "Hello, world!")
  }

  test("bind") {
    val injector = TestInjector(
      modules = Seq(TestBindModule))
      .bind[Baz](new Baz(100))
      .bind[String, Prod]("Goodbye, world!")
      .create

    assert(injector.instance[Baz].value == 100)
    assert(injector.instance[String, Prod] == "Goodbye, world!")
  }

  test("bind fails after injector is called") {
    val testInjector = TestInjector(
      modules = Seq(TestBindModule))
      .bind[Baz](new Baz(100))
    val injector = testInjector.create

    intercept[IllegalStateException] {
      testInjector.bind[String, Prod]("Goodbye, world!")
    }
    assert(injector.instance[Baz].value == 100)
    assert(injector.instance[String, Prod] == "Hello, world!")
  }
}
