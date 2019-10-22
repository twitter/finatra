package com.twitter.inject.tests.utils

import com.twitter.inject.Test
import com.twitter.inject.annotations._
import com.twitter.inject.utils.AnnotationUtils
import java.lang.annotation.Annotation

class AnnotationUtilsTest extends Test {

  test("AnnotationUtils#filterIfAnnotationPresent") {
    val annotationMap: Map[String, Seq[Annotation]] =
      AnnotationUtils.findAnnotations(classOf[CaseClassOneTwo], Seq("one", "two"))
    annotationMap.isEmpty should be(false)
    val annotations = annotationMap.flatMap { case (_, annotations) => annotations }.toSeq

    val found = AnnotationUtils.filterIfAnnotationPresent[MarkerAnnotation](annotations)
    found.size should be(1)
    found.head.annotationType should equal(classOf[Annotation2])
  }

  test("AnnotationUtils#filterAnnotations") {
    val annotationMap: Map[String, Seq[Annotation]] =
      AnnotationUtils.findAnnotations(classOf[CaseClassThreeFour], Seq("three", "four"))
    annotationMap.isEmpty should be(false)
    val annotations = annotationMap.flatMap { case (_, annotations) => annotations }.toSeq

    val filterSet: Set[Class[_ <: Annotation]] = Set(classOf[Annotation4])
    val found = AnnotationUtils.filterAnnotations(filterSet, annotations)
    found.size should be(1)
    found.head.annotationType should equal(classOf[Annotation4])
  }

  test("AnnotationUtils#findAnnotation") {
    val annotationMap: Map[String, Seq[Annotation]] =
      AnnotationUtils.findAnnotations(classOf[CaseClassThreeFour], Seq("three", "four"))
    annotationMap.isEmpty should be(false)
    val annotations = annotationMap.flatMap { case (_, annotations) => annotations }.toSeq

    AnnotationUtils.findAnnotation(classOf[Annotation1], annotations) should be(None) // not found
    val found = AnnotationUtils.findAnnotation(classOf[Annotation3], annotations)
    found.isDefined should be(true)
    found.get.annotationType() should equal(classOf[Annotation3])
  }

  test("AnnotationUtils#findAnnotation by type") {
    val annotationMap: Map[String, Seq[Annotation]] =
      AnnotationUtils.findAnnotations(
        classOf[CaseClassOneTwoThreeFour],
        Seq("one", "two", "three", "four"))
    annotationMap.isEmpty should be(false)
    val annotations = annotationMap.flatMap { case (_, annotations) => annotations }.toSeq

    AnnotationUtils.findAnnotation[MarkerAnnotation](annotations) should be(None) // not found
    AnnotationUtils.findAnnotation[Annotation1](annotations).isDefined should be(true)
    AnnotationUtils.findAnnotation[Annotation2](annotations).isDefined should be(true)
    AnnotationUtils.findAnnotation[Annotation3](annotations).isDefined should be(true)
    AnnotationUtils.findAnnotation[Annotation4](annotations).isDefined should be(true)
  }

  test("AnnotationUtils#annotationEquals") {
    val annotationMap: Map[String, Seq[Annotation]] =
      AnnotationUtils.findAnnotations(
        classOf[CaseClassOneTwoThreeFour],
        Seq("one", "two", "three", "four"))
    annotationMap.isEmpty should be(false)
    val annotations = annotationMap.flatMap { case (_, annotations) => annotations }.toSeq

    val found = AnnotationUtils.findAnnotation[Annotation1](annotations)
    found.isDefined should be(true)

    val annotation = found.get
    AnnotationUtils.annotationEquals[Annotation1](annotation) should be(true)
  }

  test("AnnotationUtils#isAnnotationPresent") {
    val annotationMap: Map[String, Seq[Annotation]] =
      AnnotationUtils.findAnnotations(
        classOf[CaseClassOneTwoThreeFour],
        Seq("one", "two", "three", "four"))
    annotationMap.isEmpty should be(false)
    val annotations = annotationMap.flatMap { case (_, annotations) => annotations }.toSeq

    val annotation1 = AnnotationUtils.findAnnotation[Annotation1](annotations).get
    val annotation2 = AnnotationUtils.findAnnotation[Annotation2](annotations).get
    val annotation3 = AnnotationUtils.findAnnotation[Annotation3](annotations).get
    val annotation4 = AnnotationUtils.findAnnotation[Annotation4](annotations).get

    AnnotationUtils.isAnnotationPresent[MarkerAnnotation](annotation1) should be(false)
    AnnotationUtils.isAnnotationPresent[MarkerAnnotation](annotation2) should be(true)
    AnnotationUtils.isAnnotationPresent[MarkerAnnotation](annotation3) should be(true)
    AnnotationUtils.isAnnotationPresent[MarkerAnnotation](annotation4) should be(false)
  }

  test("AnnotationUtils#findAnnotations") {
    var found: Map[String, Seq[Annotation]] =
      AnnotationUtils.findAnnotations(classOf[WithThings], Seq("thing1", "thing2"))
    found.isEmpty should be(false)
    var annotations = found.flatMap { case (_, annotations) => annotations }.toSeq
    annotations.size should equal(4)

    found =
      AnnotationUtils.findAnnotations(classOf[WithWidgets], Seq("widget1", "widget2"))
    found.isEmpty should be(false)
    annotations = found.flatMap { case (_, annotations) => annotations }.toSeq
    annotations.size should equal(4)

    found =
      AnnotationUtils.findAnnotations(classOf[CaseClassOneTwo], Seq("one", "two"))
    found.isEmpty should be(false)
    annotations = found.flatMap { case (_, annotations) => annotations }.toSeq
    annotations.size should equal(2)

    found =
      AnnotationUtils.findAnnotations(classOf[CaseClassThreeFour], Seq("three", "four"))
    found.isEmpty should be(false)
    annotations = found.flatMap { case (_, annotations) => annotations }.toSeq
    annotations.size should equal(2)

    found =
      AnnotationUtils.findAnnotations(classOf[CaseClassOneTwoThreeFour], Seq("one", "two", "three", "four"))
    found.isEmpty should be(false)
    annotations = found.flatMap { case (_, annotations) => annotations }.toSeq
    annotations.size should equal(4)
  }

  test("AnnotationUtils#getValueIfAnnotatedWith") {
    val found: Map[String, Seq[Annotation]] =
      AnnotationUtils.findAnnotations(classOf[WithThings], Seq("thing1", "thing2"))
    found.isEmpty should be(false)
    val annotations = found.flatMap { case (_, annotations) => annotations }.toSeq

    val things = AnnotationUtils.filterAnnotations(Set(classOf[Thing]), annotations)
    things.foreach { thing =>
      AnnotationUtils.getValueIfAnnotatedWith[MarkerAnnotation](thing).isDefined should be(true)
    }

    // @Annotation1 is not annotated with @MarkerAnnotation
    // @Annotation2 is annotated with @MarkerAnnotation but does not define a value() function
    AnnotationUtils.getValueIfAnnotatedWith[MarkerAnnotation](
      AnnotationUtils.findAnnotation[Annotation1](annotations).get
    ).isDefined should be(false)
    AnnotationUtils.getValueIfAnnotatedWith[MarkerAnnotation](
      AnnotationUtils.findAnnotation[Annotation2](annotations).get
    ).isDefined should be(false)
  }
}
