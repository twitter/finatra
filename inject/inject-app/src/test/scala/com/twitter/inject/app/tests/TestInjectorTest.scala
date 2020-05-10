package com.twitter.inject.app.tests

import com.google.inject.Provides
import com.google.inject.name.Names
import com.twitter.app.GlobalFlag
import com.twitter.finagle.Service
import com.twitter.inject.annotations.{Annotations, Down, Flag, Flags, Up}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, Mockito, Test, TwitterModule, TypeUtils}
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}
import scala.language.higherKinds
import scala.reflect.runtime.universe._

object testBooleanGlobalFlag
    extends GlobalFlag[Boolean](false, "Test boolean global flag defaulted to false")
object testStringGlobalFlag
    extends GlobalFlag[String]("foo", "Test string global flag defaulted to foo")
object testMapGlobalFlag
    extends GlobalFlag[Map[String, String]](
      Map.empty,
      "Test map global flag defaulted to Map.empty"
    )

class FooWithInject @Inject() (@Flag("x") x: Boolean) {
  def bar: Boolean = x
}

class WithInjector @Inject() (injector: Injector) {
  // use with the BooleanFlagModule
  def assertFlag(bool: Boolean): Unit =
    assert(injector.instance[Boolean](Flags.named("x")) == bool)
}

class Bar {
  // read global flags
  val booleanGlobalFlag: Boolean = testBooleanGlobalFlag()
  val stringGlobalFlag: String = testStringGlobalFlag()
  val mapGlobalFlag: Map[String, String] = testMapGlobalFlag()
}

trait DoEverything[+MM[_]] {
  def uppercase(msg: String): MM[String]
  def echo(msg: String): MM[String]
  def magicNum(): MM[String]
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

class DoEverythingImpl42 extends DoEverything[Future] {
  override def uppercase(msg: String): Future[String] = {
    Future.value(msg.toUpperCase)
  }

  override def echo(msg: String): Future[String] = {
    Future.value(msg)
  }

  override def magicNum(): Future[String] = {
    Future.value("42")
  }
}

class DoEverythingImpl137 extends DoEverything[Future] {
  override def uppercase(msg: String): Future[String] = {
    Future.value(msg.toUpperCase)
  }

  override def echo(msg: String): Future[String] = {
    Future.value(msg)
  }

  override def magicNum(): Future[String] = {
    Future.value("137")
  }
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
    bind[Service[Int, String]].toInstance(Service.mk { i: Int =>
      Future.value(s"The answer is: $i")
    })
    bind[Option[Boolean]].toInstance(None)
    bind[Seq[Long]].toInstance(Seq(1L, 2L, 3L))
    // need to explicitly provide a Manifest for the higher kinded type
    bind[DoEverything[Future]](TypeUtils.asManifest[DoEverything[Future]])
      .toInstance(new DoEverythingImpl42)
  }

  @Provides
  @Singleton
  def providesOptionalService: Option[Service[String, String]] = {
    Some(Service.mk { name: String => Future.value(s"Hello $name!") })
  }
}

class TestInjectorTest extends Test with Mockito {

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
    injector.instance[Seq[Long]] should equal(Seq(1L, 2L, 3L))
    val svc = injector.instance[Service[Int, String]]
    await(svc(42)) should equal("The answer is: 42")
    // need to explicitly provide a Manifest here for the higher kinded type
    // note: this is not always necessary
    await(
      injector
        .instance[DoEverything[Future]](
          TypeUtils.asManifest[DoEverything[Future]]).magicNum()) should equal("42")
  }

  test("bind") {
    val testMap: Map[Number, Processor] =
      Map(new FortyTwo -> new ProcessorB, new ThirtyThree -> new ProcessorA)

    val mockService: Service[Int, String] = mock[Service[Int, String]]
    mockService.apply(anyInt).returns(Future.value("hello, world"))

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
      .bind[Map[Number, Processor]].toInstance(testMap)
      .bind[Service[Int, String]].toInstance(mockService)
      .bind[Option[Boolean]].toInstance(Some(false))
      .bind[Option[Long]].toInstance(None)
      .bind[Option[Service[String, String]]].toInstance(None)
      .bind[Seq[Long]].toInstance(Seq(33L, 34L))
      // need to explicitly provide a TypeTag here for the higher kinded type
      // note: this is not always necessary
      .bind[DoEverything[Future]](typeTag[DoEverything[Future]]).toInstance(new DoEverythingImpl137)
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

    injector.instance[Map[Number, Processor]] should equal(testMap)
    val svc = injector.instance[Service[Int, String]]
    await(svc(1)) should equal("hello, world")

    injector.instance[Option[Boolean]] shouldBe Some(false)
    injector.instance[Option[Long]] shouldBe None

    injector.instance[Option[Service[String, String]]] shouldBe None

    // need to explicitly provide a Manifest here for the higher kinded type
    // note: this is not always necessary
    await(
      injector
        .instance[DoEverything[Future]](
          TypeUtils.asManifest[DoEverything[Future]]).magicNum()) should equal("137")
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
      .bindClass(classOf[Number]).annotatedWith(Flags.named("thirty.three")).to(
        classOf[ThirtyThree])
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
        .bind[Baz].toInstance(new Baz(100))
    val injector = testInjector.create

    intercept[IllegalStateException] {
      testInjector.bind[String].annotatedWith[Up].toInstance("Goodbye, world!")
    }
    injector.instance[Baz].value should equal(100)
    injector.instance[String, Up] should equal("Hello, world!")
  }

  test("assure the Injector is bound to the object graph") {
    val injector = TestInjector(
      modules = Seq(BooleanFlagModule),
      flags = Map("x" -> "true")
    ).create
    injector.instance[WithInjector].assertFlag(true)
  }
}
