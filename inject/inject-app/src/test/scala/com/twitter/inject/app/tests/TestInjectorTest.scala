package com.twitter.inject.app.tests

import com.twitter.app.GlobalFlag
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

class Foo @Inject()(@Flag("x") x: Boolean) {
  def bar = x
}

class Bar {
  // read global flags
  val booleanGlobalFlag = testBooleanGlobalFlag()
  val stringGlobalFlag = testStringGlobalFlag()
  val mapGlobalFlag = testMapGlobalFlag()
}

class TestInjectorTest extends Test {

  override protected def afterEach(): Unit = {
    // reset flags
    testBooleanGlobalFlag.reset()
    testStringGlobalFlag.reset()
    testMapGlobalFlag.reset()

    super.afterEach()
  }

  "TestInjector" should {

    "default boolean flags properly" in {
      val injector = TestInjector(BooleanFlagModule)
      val foo = injector.instance[Foo]
      assert(!foo.bar)
    }

    "default global flags properly" in {
      val injector = TestInjector()
      val bar = injector.instance[Bar]
      assert(!bar.booleanGlobalFlag)
      assert(bar.stringGlobalFlag == "foo")
      assert(bar.mapGlobalFlag == Map.empty)
    }

    "set global flags values properly" in {
      val injector = TestInjector(
        modules = Seq(),
        flags = Map(
          "com.twitter.inject.app.tests.testBooleanGlobalFlag" -> "true",
          "com.twitter.inject.app.tests.testStringGlobalFlag" -> "bar",
          "com.twitter.inject.app.tests.testMapGlobalFlag" -> "key1=foo,key2=bar"))
      val bar = injector.instance[Bar]
      assert(bar.booleanGlobalFlag)
      assert(bar.stringGlobalFlag == "bar")
      assert(bar.mapGlobalFlag == Map("key1" -> "foo", "key2" -> "bar"))
    }
  }
}
