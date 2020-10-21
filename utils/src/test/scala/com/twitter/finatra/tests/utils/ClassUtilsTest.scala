package com.twitter.finatra.tests.utils

import com.twitter.finatra.tests.utils.ClassUtilsTest.{ClassA, Request}
import com.twitter.finatra.tests.utils.ClassUtilsTest.has_underscore.ClassB
import com.twitter.finatra.tests.utils.ClassUtilsTest.number_1.Foo
import com.twitter.finatra.tests.utils.ClassUtilsTest.okNaming.Ok
import com.twitter.finatra.tests.utils.ClassUtilsTest.ver2_3.{
  Ext,
  Response,
  This$Breaks$In$ManyWays
}
import com.twitter.finatra.tests.utils.DoEverything.DoEverything$Client
import com.twitter.finatra.utils.{ClassUtils, FileResolver}
import com.twitter.inject.Test
import com.twitter.util.{Future, FuturePools}

trait Fungible[+T]
trait BarService
trait ToBarService {
  def toBarService: BarService
}

abstract class GeneratedFooService

trait DoEverything[+MM[_]] extends GeneratedFooService

object DoEverything extends GeneratedFooService { self =>

  trait ServicePerEndpoint extends ToBarService with Fungible[ServicePerEndpoint]

  class DoEverything$Client extends DoEverything[Future]
}

object ClassUtilsTest {
  object ver2_3 {
    // this "package" naming will blow up class.getSimpleName
    // packages with underscore then a number make Java unhappy
    final case class Ext(a: String, b: String)
    case class Response(a: String, b: String)

    class This$Breaks$In$ManyWays
  }

  object okNaming {
    case class Ok(a: String, b: String)
  }

  object has_underscore {
    case class ClassB(a: String, b: String)
  }

  object number_1 {
    // this "package" naming will blow up class.getSimpleName
    // packages with underscore then number make Java unhappy
    final case class Foo(a: String)
  }

  final case class ClassA(a: String, b: String)
  case class Request(a: String)

  class Bar
}

class ClassUtilsTest extends Test {

  test("ClassUtils#simpleName") {
    // these cause class.getSimpleName to blow up, ensure we don't
    ClassUtils.simpleName(classOf[Ext]) should equal("Ext")
    try {
      classOf[Ext].getSimpleName should equal("Ext")
    } catch {
      case _: InternalError =>
      // do nothing -- fails in JDK8 but not JDK11
    }
    ClassUtils.simpleName(classOf[Response]) should equal("Response")
    try {
      classOf[Response].getSimpleName should equal("Response")
    } catch {
      case _: InternalError =>
      // do nothing -- fails in JDK8 but not JDK11
    }

    // show we don't blow up
    ClassUtils.simpleName(classOf[Ok]) should equal("Ok")
    try {
      classOf[Ok].getSimpleName should equal("Ok")
    } catch {
      case _: InternalError =>
      // do nothing -- fails in JDK8 but not JDK11
    }

    // ensure we don't blow up
    ClassUtils.simpleName(classOf[ClassB]) should equal("ClassB")
    intercept[InternalError] {
      classOf[ClassB].getSimpleName
    }

    // this causes class.getSimpleName to blow up, ensure we don't
    ClassUtils.simpleName(classOf[Foo]) should equal("Foo")
    intercept[InternalError] {
      classOf[Foo].getSimpleName
    }

    ClassUtils.simpleName(classOf[ClassA]) should equal("ClassA")
    ClassUtils.simpleName(classOf[ClassA]) should equal(classOf[ClassA].getSimpleName)
    ClassUtils.simpleName(classOf[Request]) should equal("Request")
    ClassUtils.simpleName(classOf[Request]) should equal(classOf[Request].getSimpleName)

    ClassUtils.simpleName(classOf[java.lang.String]) should equal("String")
    ClassUtils.simpleName(classOf[java.lang.String]) should equal(
      classOf[java.lang.String].getSimpleName)
    ClassUtils.simpleName(classOf[String]) should equal("String")
    ClassUtils.simpleName(classOf[String]) should equal(classOf[String].getSimpleName)
    ClassUtils.simpleName(classOf[Int]) should equal("int")
    ClassUtils.simpleName(classOf[Int]) should equal(classOf[Int].getSimpleName)

    ClassUtils.simpleName(classOf[DoEverything$Client]) should equal("DoEverything$Client")
    ClassUtils.simpleName(classOf[DoEverything$Client]) should equal(
      classOf[DoEverything$Client].getSimpleName)

    ClassUtils.simpleName(classOf[This$Breaks$In$ManyWays]) should equal("ManyWays")
    intercept[InternalError] {
      classOf[This$Breaks$In$ManyWays].getSimpleName
    }
  }

  test("ClassUtils#isCaseClass") {
    ClassUtils.isCaseClass(classOf[Ext]) should be(true)
    ClassUtils.isCaseClass(classOf[Response]) should be(true)

    ClassUtils.isCaseClass(classOf[Ok]) should be(true)

    ClassUtils.isCaseClass(classOf[ClassB]) should be(true)

    ClassUtils.isCaseClass(classOf[Foo]) should be(true)

    ClassUtils.isCaseClass(classOf[ClassA]) should be(true)
    ClassUtils.isCaseClass(classOf[Request]) should be(true)

    ClassUtils.isCaseClass(classOf[FileResolver]) should be(false)
    ClassUtils.isCaseClass(classOf[FuturePools]) should be(false)

    ClassUtils.isCaseClass(classOf[java.lang.String]) should be(false)
    ClassUtils.isCaseClass(classOf[String]) should be(false)
    ClassUtils.isCaseClass(classOf[Int]) should be(false)

    ClassUtils.isCaseClass(classOf[String]) should be(false)
    ClassUtils.isCaseClass(classOf[Boolean]) should be(false)
    ClassUtils.isCaseClass(classOf[java.lang.Integer]) should be(false)
    ClassUtils.isCaseClass(classOf[Int]) should be(false)
    ClassUtils.isCaseClass(classOf[Double]) should be(false)
    ClassUtils.isCaseClass(classOf[Float]) should be(false)
    ClassUtils.isCaseClass(classOf[Long]) should be(false)
    ClassUtils.isCaseClass(classOf[Seq[_]]) should be(false)
    ClassUtils.isCaseClass(classOf[Seq[Ext]]) should be(false)
    ClassUtils.isCaseClass(classOf[List[_]]) should be(false)
    ClassUtils.isCaseClass(classOf[List[Ext]]) should be(false)
    ClassUtils.isCaseClass(classOf[Iterable[_]]) should be(false)
    ClassUtils.isCaseClass(classOf[Iterable[Ext]]) should be(false)
    ClassUtils.isCaseClass(classOf[Array[_]]) should be(false)
    ClassUtils.isCaseClass(classOf[Array[Ext]]) should be(false)
    ClassUtils.isCaseClass(classOf[Option[_]]) should be(false)
    ClassUtils.isCaseClass(classOf[Option[ClassB]]) should be(false)
    ClassUtils.isCaseClass(classOf[Either[_, _]]) should be(false)
    ClassUtils.isCaseClass(classOf[Either[Request, Response]]) should be(false)
    ClassUtils.isCaseClass(classOf[Map[_, _]]) should be(false)
    ClassUtils.isCaseClass(classOf[Map[ClassA, Foo]]) should be(false)
    ClassUtils.isCaseClass(classOf[Tuple1[_]]) should be(false)
    ClassUtils.isCaseClass(classOf[Tuple2[_, _]]) should be(false)
    ClassUtils.isCaseClass(classOf[(_, _, _)]) should be(false)
    ClassUtils.isCaseClass(classOf[(_, _, _, _)]) should be(false)
    ClassUtils.isCaseClass(classOf[(_, _, _, _, _)]) should be(false)
    ClassUtils.isCaseClass(classOf[(_, _, _, _, _, _)]) should be(false)
    ClassUtils.isCaseClass(classOf[Tuple7[_, _, _, _, _, _, _]]) should be(false)
    ClassUtils.isCaseClass(classOf[Tuple8[_, _, _, _, _, _, _, _]]) should be(false)
    ClassUtils.isCaseClass(classOf[Tuple9[_, _, _, _, _, _, _, _, _]]) should be(false)
    ClassUtils.isCaseClass(classOf[Tuple10[_, _, _, _, _, _, _, _, _, _]]) should be(false)
  }

  test("ClassUtils#notCaseClass") {
    ClassUtils.notCaseClass(classOf[Ext]) should be(false)
    ClassUtils.notCaseClass(classOf[Response]) should be(false)

    ClassUtils.notCaseClass(classOf[Ok]) should be(false)

    ClassUtils.notCaseClass(classOf[ClassB]) should be(false)

    ClassUtils.notCaseClass(classOf[Foo]) should be(false)

    ClassUtils.notCaseClass(classOf[ClassA]) should be(false)
    ClassUtils.notCaseClass(classOf[Request]) should be(false)

    ClassUtils.notCaseClass(classOf[FileResolver]) should be(true)
    ClassUtils.notCaseClass(classOf[FuturePools]) should be(true)

    ClassUtils.notCaseClass(classOf[java.lang.String]) should be(true)
    ClassUtils.notCaseClass(classOf[String]) should be(true)
    ClassUtils.notCaseClass(classOf[Int]) should be(true)

    ClassUtils.notCaseClass(classOf[String]) should be(true)
    ClassUtils.notCaseClass(classOf[Boolean]) should be(true)
    ClassUtils.notCaseClass(classOf[java.lang.Integer]) should be(true)
    ClassUtils.notCaseClass(classOf[Int]) should be(true)
    ClassUtils.notCaseClass(classOf[Double]) should be(true)
    ClassUtils.notCaseClass(classOf[Float]) should be(true)
    ClassUtils.notCaseClass(classOf[Long]) should be(true)
    ClassUtils.notCaseClass(classOf[Seq[_]]) should be(true)
    ClassUtils.notCaseClass(classOf[Seq[Ext]]) should be(true)
    ClassUtils.notCaseClass(classOf[List[_]]) should be(true)
    ClassUtils.notCaseClass(classOf[List[Ext]]) should be(true)
    ClassUtils.notCaseClass(classOf[Iterable[_]]) should be(true)
    ClassUtils.notCaseClass(classOf[Iterable[Ext]]) should be(true)
    ClassUtils.notCaseClass(classOf[Array[_]]) should be(true)
    ClassUtils.notCaseClass(classOf[Array[Ext]]) should be(true)
    ClassUtils.notCaseClass(classOf[Option[_]]) should be(true)
    ClassUtils.notCaseClass(classOf[Option[ClassB]]) should be(true)
    ClassUtils.notCaseClass(classOf[Either[_, _]]) should be(true)
    ClassUtils.notCaseClass(classOf[Either[Request, Response]]) should be(true)
    ClassUtils.notCaseClass(classOf[Map[_, _]]) should be(true)
    ClassUtils.notCaseClass(classOf[Map[ClassA, Foo]]) should be(true)
    ClassUtils.notCaseClass(classOf[Tuple1[_]]) should be(true)
    ClassUtils.notCaseClass(classOf[Tuple2[_, _]]) should be(true)
    ClassUtils.notCaseClass(classOf[(_, _, _)]) should be(true)
    ClassUtils.notCaseClass(classOf[(_, _, _, _)]) should be(true)
    ClassUtils.notCaseClass(classOf[(_, _, _, _, _)]) should be(true)
    ClassUtils.notCaseClass(classOf[(_, _, _, _, _, _)]) should be(true)
    ClassUtils.notCaseClass(classOf[Tuple7[_, _, _, _, _, _, _]]) should be(true)
    ClassUtils.notCaseClass(classOf[Tuple8[_, _, _, _, _, _, _, _]]) should be(true)
    ClassUtils.notCaseClass(classOf[Tuple9[_, _, _, _, _, _, _, _, _]]) should be(true)
    ClassUtils.notCaseClass(classOf[Tuple10[_, _, _, _, _, _, _, _, _, _]]) should be(true)
  }
}
