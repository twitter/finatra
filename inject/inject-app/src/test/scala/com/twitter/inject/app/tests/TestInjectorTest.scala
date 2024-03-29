package com.twitter.inject.app.tests

import com.google.inject.name.Names
import com.google.inject.Provides
import com.google.inject.Scopes
import com.google.inject.Stage
import com.twitter.app.GlobalFlag
import com.twitter.finagle.Service
import com.twitter.inject.annotations.Annotations
import com.twitter.inject.annotations.Down
import com.twitter.inject.annotations.Flag
import com.twitter.inject.annotations.Flags
import com.twitter.inject.annotations.Up
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.inject.modules.StackTransformerModule
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.Injector
import com.twitter.inject.Test
import com.twitter.inject.TwitterModule
import com.twitter.util.Future
import com.twitter.util.mock.Mockito
import javax.inject.Inject
import javax.inject.Singleton
import scala.language.higherKinds

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

class FineStructureConstant extends Number {
  private[this] val constant: Float = 1 / 137
  override def intValue(): Int = constant.toInt

  override def longValue(): Long = constant.toLong

  override def floatValue(): Float = constant

  override def doubleValue(): Double = constant.toDouble
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

trait Incr {
  def getValue: Int
}

object TestBindModule extends TwitterModule {
  private[this] var counter: Int = 0

  class IncrImpl extends Incr {
    counter += 1
    def getValue: Int = counter
  }

  flag[Boolean]("bool", false, "default is false")

  override protected def configure(): Unit = {
    bind[Incr].annotatedWith(Names.named("singleton")).to[IncrImpl].in[Singleton]
    bind[Incr]
      .annotatedWith(Names.named("nonsingleton")).to[IncrImpl] // creates a new instance every time
    bind[Incr].to(classOf[IncrImpl]) // not annotated and not scoped, creates a new instance
    bind[String].annotatedWith[Up].toInstance("Hello, world!")
    bind[Baz].toInstance(new Baz(10))
    bind[Baz].annotatedWith(Names.named("five")).toInstance(new Baz(5))
    bind[Baz].annotatedWith(Names.named("six")).toInstance(new Baz(6))
    bind[Service[Int, String]].toInstance(Service.mk { i: Int =>
      Future.value(s"The answer is: $i")
    })
    bind[Option[Boolean]].toInstance(None)
    bind[Seq[Long]].toInstance(Seq(1L, 2L, 3L))
    bind[DoEverything[Future]].toInstance(new DoEverythingImpl42)
    bind[Number].annotatedWith(Names.named("alpha")).to[FineStructureConstant].in(Scopes.SINGLETON)
  }

  @Provides
  @Singleton
  def providesOptionalService: Option[Service[String, String]] = {
    Some(Service.mk { name: String => Future.value(s"Hello $name!") })
  }
}

class TestLifecycleHooksModule extends TwitterModule {

  var started: Boolean = false
  var warmedUp: Boolean = false
  var shutDown: Boolean = false
  var closed: Boolean = false

  onExit { closed = true }

  override def singletonStartup(injector: Injector): Unit = {
    started = true
  }

  override def singletonPostWarmupComplete(injector: Injector): Unit = {
    warmedUp = true
  }

  override def singletonShutdown(injector: Injector): Unit = {
    shutDown = true
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

  test("test constructors") {
    val flags: Map[String, String] = Map("foo" -> "bar", "baz" -> "bus")

    TestInjector()
    TestInjector(StackTransformerModule, StatsReceiverModule)
    TestInjector(Seq(StackTransformerModule, StatsReceiverModule))
    TestInjector(modules = Seq(StackTransformerModule, StatsReceiverModule))
    TestInjector(Seq(StackTransformerModule, StatsReceiverModule), flags)
    TestInjector(modules = Seq(StackTransformerModule, StatsReceiverModule), flags = flags)
    TestInjector(
      Seq(StackTransformerModule, StatsReceiverModule),
      flags,
      Seq(InMemoryStatsReceiverModule))
    TestInjector(
      modules = Seq(StackTransformerModule, StatsReceiverModule),
      flags = flags,
      overrideModules = Seq(InMemoryStatsReceiverModule))
    TestInjector(
      Seq(StackTransformerModule, StatsReceiverModule),
      flags,
      Seq(InMemoryStatsReceiverModule),
      Stage.PRODUCTION)
    TestInjector(
      modules = Seq(StackTransformerModule, StatsReceiverModule),
      flags = flags,
      overrideModules = Seq(InMemoryStatsReceiverModule),
      stage = Stage.PRODUCTION)
  }

  test("lifecycle hooks") {
    val m = new TestLifecycleHooksModule
    val injector = TestInjector(m).create()

    m.started should be(false)
    m.warmedUp should be(false)

    injector.start()

    m.started should be(true)
    m.warmedUp should be(true)
    m.shutDown should be(false)
    m.closed should be(false)

    injector.close()

    m.shutDown should be(true)
    m.closed should be(true)
  }

  test("can not be started twice") {
    val injector = TestInjector().create()
    injector.start()
    intercept[IllegalStateException](injector.start())
  }

  test("can not be closed twice") {
    val injector = TestInjector().create()
    injector.close()
    intercept[IllegalStateException](injector.close())
  }

  test("can not be used after closed") {
    val injector = TestInjector().create()
    injector.close()
    intercept[IllegalStateException](injector.instance[Baz])
  }

  test("default boolean flags properly") {
    val injector = TestInjector(BooleanFlagModule).create()
    injector.instance[FooWithInject].bar should be(false)
    injector.instance[Boolean](Flags.named("x")) should be(false)
  }

  test("default global flags properly") {
    val bar = TestInjector().create().instance[Bar]
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
    ).create()
    val bar = injector.instance[Bar]
    bar.booleanGlobalFlag should be(true)
    bar.stringGlobalFlag should equal("bar")
    bar.mapGlobalFlag should equal(Map("key1" -> "foo", "key2" -> "bar"))
  }

  test("module defaults") {
    val injector = TestInjector(modules = Seq(TestBindModule)).create()

    injector.instance[Baz].value should equal(10)
    injector.instance[Baz]("five").value should equal(5)
    injector.instance[Baz](Names.named("six")).value should equal(6)
    injector.instance[String, Up] should equal("Hello, world!")
    injector.instance[Seq[Long]] should equal(Seq(1L, 2L, 3L))
    val svc = injector.instance[Service[Int, String]]
    await(svc(42)) should equal("The answer is: 42")
    await(injector.instance[DoEverything[Future]].magicNum()) should equal("42")

    // the singleton version should get initialized to 1 and remain 1
    injector.instance[Incr](Names.named("singleton")).getValue should equal(1)
    injector.instance[Incr](Names.named("singleton")).getValue should equal(1)
    injector.instance[Incr](Names.named("singleton")).getValue should equal(1)

    // the non singleton version should get initialized to 2 and incr everytime requested since it is a new instance
    injector.instance[Incr](Names.named("nonsingleton")).getValue should equal(2)
    injector.instance[Incr](Names.named("nonsingleton")).getValue should equal(3)
    injector.instance[Incr](Names.named("nonsingleton")).getValue should equal(4)

    // non singleton version, not annotated and bound by classOf, should be initialized to 5 and incr everytime
    injector.instance[Incr].getValue should equal(5)
    injector.instance[Incr].getValue should equal(6)
    injector.instance[Incr].getValue should equal(7)

    // singleton shouldn't change
    injector.instance[Number](Names.named("alpha")).floatValue() should equal(1 / 137)
    injector.instance[Number](Names.named("alpha")).doubleValue() should equal((1 / 137).toDouble)
    injector.instance[Number](Names.named("alpha")).longValue() should equal((1 / 137).toLong)
    injector.instance[Number](Names.named("alpha")).intValue() should equal((1 / 137).toInt)

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
      .bind[DoEverything[Future]].toInstance(new DoEverythingImpl137)
      .create()

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

    await(injector.instance[DoEverything[Future]].magicNum()) should equal("137")
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
      .create()

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

  test("assure the Injector is bound to the object graph") {
    val injector = TestInjector(
      modules = Seq(BooleanFlagModule),
      flags = Map("x" -> "true")
    ).create()
    injector.instance[WithInjector].assertFlag(true)
  }
}
