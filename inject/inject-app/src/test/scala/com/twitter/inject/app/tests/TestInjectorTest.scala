package com.twitter.inject.app.tests

import com.google.inject.name.Names
import com.twitter.app.GlobalFlag
import com.twitter.inject.annotations.{Flag, Flags}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Test, TwitterModule}
import javax.inject.Inject

object testBooleanGlobalFlag
    extends GlobalFlag[Boolean](false, "Test boolean global flag defaulted to false")
object testStringGlobalFlag
    extends GlobalFlag[String]("foo", "Test string global flag defaulted to foo")
object testMapGlobalFlag
    extends GlobalFlag[Map[String, String]](
      Map.empty,
      "Test map global flag defaulted to Map.empty"
    )

object BooleanFlagModule extends TwitterModule {
  flag[Boolean]("x", false, "default to false")
}

object TestBindModule extends TwitterModule {
  flag[Boolean]("bool", false, "default is false")

  override protected def configure(): Unit = {
    bind[String, Up].toInstance("Hello, world!")
    bind[Baz].toInstance(new Baz(10))
    bind[Baz](Names.named("five")).toInstance(new Baz(5))
    bind[Baz](Names.named("six")).toInstance(new Baz(6))
    bind[Boolean](Flags.named("bool")).toInstance(true)
  }
}

class FooWithInject @Inject()(@Flag("x") x: Boolean) {
  def bar = x
}

class Bar {
  // read global flags
  val booleanGlobalFlag = testBooleanGlobalFlag()
  val stringGlobalFlag = testStringGlobalFlag()
  val mapGlobalFlag = testMapGlobalFlag()
}

trait TestTrait {
  def foobar(): String
}

class TestTraitImpl1 extends TestTrait {
  def foobar(): String = {
    this.getClass.getSimpleName
  }
}

class TestTraitImpl2 extends TestTrait {
  def foobar(): String = {
    this.getClass.getSimpleName
  }
}

class FortyTwo extends Number {
  private[this] val int = 42
  override def intValue(): Int = int

  override def floatValue(): Float = int.toFloat

  override def doubleValue(): Double = int.toDouble

  override def longValue(): Long = int.toLong
}

class ThirtyThree extends Number {
  private[this] val int = 33
  override def intValue(): Int = int

  override def floatValue(): Float = int.toFloat

  override def doubleValue(): Double = int.toDouble

  override def longValue(): Long = int.toLong
}

trait Processor {
  def process: String
}

class ProcessorA extends Processor {
  def process: String = this.getClass.getSimpleName
}

class ProcessorB extends Processor {
  def process: String = this.getClass.getSimpleName
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
    injector.instance[FooWithInject].bar should be(false)
    injector.instance[Boolean](Flags.named("x")) should be(false)
  }

  test("default global flags properly") {
    val bar = TestInjector().create.instance[Bar]
    bar.booleanGlobalFlag should be(false)
    bar.stringGlobalFlag should equal("foo")
    bar.mapGlobalFlag should equal(Map.empty)
  }

  test("set global flags values properly") {
    val injector = TestInjector(
      modules = Seq(),
      flags = Map(
        "com.twitter.inject.app.tests.testBooleanGlobalFlag" -> "true",
        "com.twitter.inject.app.tests.testStringGlobalFlag" -> "bar",
        "com.twitter.inject.app.tests.testMapGlobalFlag" -> "key1=foo,key2=bar"
      )
    ).create
    val bar = injector.instance[Bar]
    bar.booleanGlobalFlag should be(true)
    bar.stringGlobalFlag should equal("bar")
    bar.mapGlobalFlag should equal(Map("key1" -> "foo", "key2" -> "bar"))
  }

  test("module defaults") {
    val injector = TestInjector(modules = Seq(TestBindModule)).create

    injector.instance[Baz].value should equal(10)
    injector.instance[Baz]("five").value should equal(5)
    injector.instance[Baz](Names.named("six")).value should equal(6)
    injector.instance[String, Up] should equal("Hello, world!")
  }

  test("bind deprecated") {
    val injector = TestInjector(modules = Seq(TestBindModule))
      .bind[Baz](new Baz(100))
      .bind[String, Up]("Goodbye, world!")
      .bind[String](Names.named("foo"), "bar")
      .bind[String](Flags.named("cat.flag"), "Kat")
      .create

    injector.instance[Baz].value should equal(100)
    injector.instance[String, Up] should equal("Goodbye, world!")
    injector.instance[String]("foo") should equal("bar")
    injector.instance[String](Names.named("foo")) should equal("bar")
    injector.instance[String](Flags.named("cat.flag")) should be("Kat")
  }

  test("bind") {
    // bind[T] to [T]
    // bind[T] to (clazz)
    // bind[T] toInstance (instance)

    // bind[T] annotatedWith [T] to [T]
    // bind[T] annotatedWith (clazz) to [T]
    // bind[T] annotatedWith (annotation) to [T]

    // bind[T] annotatedWith [T] to (clazz)
    // bind[T] annotatedWith (clazz) to (clazz)
    // bind[T] annotatedWith (annotation) to (clazz)

    // bind[T] annotatedWith [T] toInstance (instance)
    // bind[T] annotatedWith (clazz) toInstance (instance)
    // bind[T] annotatedWith (annotation) toInstance (instance)
    val injector = TestInjector(modules = Seq(TestBindModule))
      .bind[TestTrait].to[TestTraitImpl1]
      .bind[Processor].to(classOf[ProcessorA])
      .bind[Baz].toInstance(new Baz(100))

      .bind[TestTrait].annotatedWith[Up].to[TestTraitImpl2]
      .bind[Number].annotatedWith(classOf[Down]).to[FortyTwo]
      .bind[Processor].annotatedWith(Flags.named("process.impl")).to[ProcessorB]

      .bind[TestTrait].annotatedWith[Down].to(classOf[TestTraitImpl1])
      .bind[Processor].annotatedWith(classOf[Up]).to(classOf[ProcessorA])
      .bind[Number].annotatedWith(Flags.named("magicNumber")).to(classOf[ThirtyThree])

      .bind[String].annotatedWith[Down].toInstance("Goodbye, world!")
      .bind[String].annotatedWith(classOf[Up]).toInstance("Very important Up String")
      .bind[String].annotatedWith(Flags.named("cat.flag")).toInstance("Kat")
      .create

    injector.instance[TestTrait].foobar() should be("TestTraitImpl1")
    injector.instance[Processor].process should be("ProcessorA")
    injector.instance[Baz].value should equal(100)

    injector.instance[TestTrait, Up].foobar() should be("TestTraitImpl2")
    injector.instance[Number](classOf[Down]).intValue() should equal(42)
    injector.instance[Number, Down].intValue() should equal(42)
    injector.instance[Processor](Flags.named("process.impl")).process should be("ProcessorB")

    injector.instance[TestTrait, Down].foobar() should be("TestTraitImpl1")
    injector.instance[Processor](classOf[Up]).process should be("ProcessorA")
    injector.instance[Processor, Up].process should be("ProcessorA")
    injector.instance[Number](Flags.named("magicNumber")).intValue() should equal(33)

    injector.instance[String, Down] should equal("Goodbye, world!")
    injector.instance[String](classOf[Up]) should equal("Very important Up String")
    injector.instance[String, Up] should equal("Very important Up String")
    injector.instance[String](Flags.named("cat.flag")) should be("Kat")
  }

  test("bindClass") {
    // bindClass to [T] -- is this a legit case?
    // bindClass to (clazz)
    // bindClass toInstance (instance)

    // bindClass annotatedWith (clazz) to [T] -- is this a legit case?
    // bindClass annotatedWith (annotation) to [T] -- is this a legit case?

    // bindClass annotatedWith (clazz) to (clazz)
    // bindClass annotatedWith (annotation) to (clazz)

    // bindClass annotatedWith (clazz) toInstance (instance)
    // bindClass annotatedWith (annotation) toInstance (instance)
    val injector = TestInjector(modules = Seq(TestBindModule))
      .bindClass(classOf[TestTrait]).to[TestTraitImpl2]
      .bindClass(classOf[Processor]).to(classOf[ProcessorA])
      .bindClass(classOf[Double], 3.14d)

      .bindClass(classOf[Processor]).annotatedWith(classOf[Up]).to[ProcessorB]
      .bindClass(classOf[Number]).annotatedWith(Flags.named("forty.two")).to[FortyTwo]

      .bindClass(classOf[TestTrait]).annotatedWith(classOf[Up]).to[TestTraitImpl1]
      .bindClass(classOf[Number]).annotatedWith(Flags.named("thirty.three")).to(classOf[ThirtyThree])

      .bindClass(classOf[String]).annotatedWith(classOf[Down]).toInstance("Lorem ipsum")
      .bindClass(classOf[String]).annotatedWith(Flags.named("dog.flag")).toInstance("Fido")
      .create

    injector.instance[TestTrait].foobar() should be("TestTraitImpl2")
    injector.instance(classOf[TestTrait]).foobar() should be("TestTraitImpl2")
    injector.instance[Processor].process should be("ProcessorA")
    injector.instance(classOf[Processor]).process should be("ProcessorA")
    injector.instance[Double] should equal(3.14d)
    injector.instance(classOf[Double]) should equal(3.14d)

    injector.instance[Processor, Up].process should be("ProcessorB")
    injector.instance(classOf[Processor], Annotations.up()).process should be("ProcessorB")
    injector.instance[Number](Flags.named("forty.two")).intValue() should equal(42)
    injector.instance(classOf[Number], Flags.named("forty.two")).intValue() should equal(42)

    injector.instance(classOf[TestTrait], Annotations.up()).foobar() should be("TestTraitImpl1")
    injector.instance[TestTrait](Annotations.up()).foobar() should be("TestTraitImpl1")
    injector.instance(classOf[Number], Flags.named("thirty.three")).intValue() should equal(33)
    injector.instance[Number](Flags.named("thirty.three")).intValue() should equal(33)

    injector.instance[String](Annotations.down()) should be("Lorem ipsum")
    injector.instance(classOf[String], Annotations.down()) should be("Lorem ipsum")
    injector.instance[String](Flags.named("dog.flag")) should be("Fido")
    injector.instance(classOf[String], Flags.named("dog.flag")) should be("Fido")
  }

  test("bind fails after injector is called") {
    val testInjector =
      TestInjector(modules = Seq(TestBindModule))
        .bind[Baz](new Baz(100))
    val injector = testInjector.create

    intercept[IllegalStateException] {
      testInjector.bind[String, Up]("Goodbye, world!")
    }
    injector.instance[Baz].value should equal(100)
    injector.instance[String, Up] should equal("Hello, world!")
  }
}
