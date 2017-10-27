
package com.twitter.finatra.json.tests.internal.caseclass.jackson

import com.twitter.finatra.json.internal.caseclass.reflection.CaseClassSigParser
import com.twitter.inject.Test

case class RootSorted(m: scala.collection.SortedMap[String, String])
case class ImmutableSorted(m: scala.collection.immutable.SortedMap[String, String])
case class RootSeq(m: scala.collection.Seq[String])
case class ImmutableSeq(m: scala.collection.immutable.Seq[String])
case class MutableSeq(m: scala.collection.mutable.Seq[String])

class ScalaTypeTest extends Test {

  test("All kinds of SortedMap are subclasses of root Map") {
    classOf[scala.collection.Map[Any, Any]].
      isAssignableFrom(classOf[scala.collection.SortedMap[Any, Any]]) should be(true)

    classOf[scala.collection.Map[Any, Any]].
      isAssignableFrom(classOf[scala.collection.immutable.SortedMap[Any, Any]]) should be(true)
  }

  test("ScalaType.isMap returns true if object is root SortedMap") {
    val foo = RootSorted(scala.collection.SortedMap("" -> ""))
    val f = CaseClassSigParser.parseConstructorParams(foo.getClass)
    f.head.scalaType.isMap should be(true)
  }

  test("ScalaType.isMap returns true if object is immutable SortedMap") {
    val foo = ImmutableSorted(scala.collection.immutable.SortedMap("" -> ""))
    val f = CaseClassSigParser.parseConstructorParams(foo.getClass)
    f.head.scalaType.isMap should be(true)
  }

  test("All kinds of Seq are subclasses of root Iterable") {
    classOf[Iterable[Any]].
      isAssignableFrom(classOf[scala.collection.Seq[Any]]) should be(true)

    classOf[Iterable[Any]].
      isAssignableFrom(classOf[scala.collection.immutable.Seq[Any]]) should be(true)

    classOf[Iterable[Any]].
      isAssignableFrom(classOf[scala.collection.mutable.Seq[Any]]) should be(true)
  }

  test("ScalaType.isCollection returns true if object is root Seq") {
    val foo = RootSeq(scala.collection.Seq(""))
    val f = CaseClassSigParser.parseConstructorParams(foo.getClass)
    f.head.scalaType.isCollection should be(true)
  }

  test("ScalaType.isCollection returns true if object is immutable Seq") {
    val foo = ImmutableSeq(scala.collection.immutable.Seq(""))
    val f = CaseClassSigParser.parseConstructorParams(foo.getClass)
    f.head.scalaType.isCollection should be(true)
  }

  test("ScalaType.isCollection returns true if object is mutable Seq") {
    val foo = MutableSeq(scala.collection.mutable.Seq(""))
    val f = CaseClassSigParser.parseConstructorParams(foo.getClass)
    f.head.scalaType.isCollection should be(true)
  }
}

