package com.twitter.inject.tests

import com.twitter.inject.{Test, TypeUtils}
import org.scalacheck.Arbitrary
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scala.reflect.runtime.universe._

trait A
trait B

class TypeUtilsTest extends Test with ScalaCheckDrivenPropertyChecks {

  test("asManifest handles AnyVal/Any/Null/Nothing") {
    forAll(Arbitrary.arbAnyVal.arbitrary) { anyVal =>
      assert(manifestedTypesEquals(anyVal))
    }
    assert(TypeUtils.asManifest[AnyVal] == manifest[AnyVal])
    assert(TypeUtils.asManifest[Any] == manifest[Any])
    assert(TypeUtils.asManifest[Null] == manifest[Null])
    assert(TypeUtils.asManifest[Nothing] == manifest[Nothing])
  }

  test("asManifest handles NoClassDefFound exceptions") {
    val t = typeTag[A with B]
    intercept[NoClassDefFoundError] {
      t.mirror.runtimeClass(t.tpe)
    }
    assert(TypeUtils.asManifest[A with B] == manifest[Any])
  }

  def manifestedTypesEquals[T: TypeTag: Manifest](a: T): Boolean = {
    TypeUtils.asManifest[T] == manifest[T]
  }

}
