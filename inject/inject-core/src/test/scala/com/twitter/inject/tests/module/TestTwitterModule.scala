package com.twitter.inject.tests.module

import com.google.inject.name.{Named, Names}
import com.google.inject.spi.TypeConverter
import com.google.inject.{Provides, TypeLiteral}
import com.twitter.app.Flag
import com.twitter.conversions.DurationOps._
import com.twitter.inject.tests.Prod
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import java.util.Properties
import scala.util.control.NonFatal

object TestTwitterModule extends TwitterModule {

  // Note: The following flag values are not used in this module, but are @Flag injected elsewhere
  flag("moduleMagicNum", "30", "Module Magic number")
  flag("moduleDuration", 5.seconds, "Module duration")

  val localModuleFlag: Flag[String] = flag("moduleString", "default", "A passed string")

  override protected def configure(): Unit = {
    bindSingleton[String](Names.named("str1")).toInstance("string1")
    bind[String].annotatedWith(Names.named("str2")).toInstance("string2")

    bindSingleton[Int].toInstance(11)
    bind[String].toInstance("default string")
    bind[String].annotatedWith[Prod].toInstance("prod string")

    bind[MyServiceInterface].to[MyServiceImpl].in[Singleton]

    bindAssistedFactory[ComplexServiceFactory]()
    getProvider[Int]

    bindMultiple[MultiService].addBinding.to[OneMultiService]
    bindMultiple[MultiService].addBinding.to[TwoMultiService]

    bindOption[String](Names.named("SomeString"))
    bindOption[Double](Names.named("OptionalDouble")).setDefault.toInstance(142d)
    bindOption[Float].setBinding.toInstance(137f)
    bindOption[String](Names.named("DoEverythingString")).setBinding.toInstance("NOTDEFAULT")

    addTypeConverter[ClassToConvert](new TypeConverter {
      override def convert(s: String, typeLiteral: TypeLiteral[_]): AnyRef = {
        ClassToConvert(s)
      }
    })

    val properties = new Properties()
    properties.setProperty("name", "Steve")
    Names.bindProperties(binder(), properties)
  }

  override def singletonStartup(injector: Injector): Unit = {
    assert(!localModuleFlag.isDefined) // isDefined is only true when a value has been parsed, will be false if the flag still has the default
    assert(localModuleFlag() == "default") // no flag parsing happens so the flag will be the default value, this emits a warning of "SEVERE: Flag moduleString read before parse."

    assert(injector.instance[String, Prod] == "prod string")
    assert(injector.instance[String, Prod] == "prod string")

    // check that the Set was bound with two elements
    val services = injector.instance[Set[MultiService]]
    assert(services.size == 2)
    services.foreach {
      case _: OneMultiService => assert(true)
      case _: TwoMultiService => assert(true)
      case _ => assert(false)
    }

    // binder created with no binding, lookup of Option[String] should return None
    assert(injector.instance[Option[String]](Names.named("SomeString")).isEmpty)

    // binds the Option[Double]
    assert(injector.instance[Option[Double]](Names.named("OptionalDouble")).isDefined)
    assert(injector.instance[Option[Double]](Names.named("OptionalDouble")).get == 142d)
    // should also bind the Double, since we called ".setDefault"
    assert(injector.instance[Double](Names.named("OptionalDouble")) == 142d)

    assert(injector.instance[Option[Float]].isDefined)
    assert(injector.instance[Option[Float]].get == 137f)

    // no binder was ever created here
    try {
      injector.instance[Option[String]](Names.named("MyString"))
      assert(false)
    } catch {
      case NonFatal(e) =>
        // ensure that the DoEverythingModule is in the stack trace
        assert(e.getStackTrace.exists(_.getClassName == TestTwitterModule.getClass.getName))
    }

    // binds the Option[String]
    assert(injector.instance[Option[String]](Names.named("DoEverythingString")).isDefined)
    assert(injector.instance[Option[String]](Names.named("DoEverythingString")).get == "NOTDEFAULT")
    // should also bind the String, since we called ".setBinding"
    assert(injector.instance[String](Names.named("DoEverythingString")) == "NOTDEFAULT")

    val myService = injector.instance[MyServiceInterface]
    val myService2 = injector.instance[MyServiceInterface]
    assert(myService.toString == myService2.toString)
  }

  override def singletonPostWarmupComplete(injector: Injector): Unit = {
    info("module post warmup complete")
  }

  override def singletonShutdown(injector: Injector): Unit = {
    info("shutdown")
  }

  @Provides
  @Named("example")
  def provideNamedString: String = {
    "named"
  }
}

case class ClassToConvert(name: String)
