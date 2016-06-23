package com.twitter.finatra.json.internal.caseclass.utils

import java.lang.annotation.Annotation

private[finatra] object AnnotationUtils {

  def filterIfAnnotationPresent[A <: Annotation : Manifest](annotations: Seq[Annotation]): Seq[Annotation] = {
    annotations filter { annot =>
      isAnnotationPresent[A](annot)
    }
  }

  def filterAnnotations(filterSet: Set[Class[_ <: Annotation]], annotations: Seq[Annotation]): Seq[Annotation] = {
    annotations filter { annotation =>
      filterSet.contains(annotation.annotationType)
    }
  }

  def findAnnotation(target: Class[_ <: Annotation], annotations: Seq[Annotation]): Option[Annotation] = {
    annotations find { annotation =>
      annotation.annotationType() == target
    }
  }

  def findAnnotation[A <: Annotation : Manifest](annotations: Seq[Annotation]): Option[A] = {
    annotations collectFirst {
      case annotation if annotationEquals[A](annotation) =>
        annotation.asInstanceOf[A]
    }
  }

  def annotationEquals[A <: Annotation : Manifest](annotation: Annotation): Boolean = {
    annotation.annotationType() == manifest[A].runtimeClass.asInstanceOf[Class[A]]
  }

  def isAnnotationPresent[A <: Annotation : Manifest](annotation: Annotation): Boolean = {
    annotation.annotationType.isAnnotationPresent(
      manifest[A].runtimeClass.asInstanceOf[Class[A]])
  }
}
