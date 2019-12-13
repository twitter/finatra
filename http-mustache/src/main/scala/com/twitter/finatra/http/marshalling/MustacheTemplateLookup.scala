package com.twitter.finatra.http.marshalling

import com.twitter.finatra.http.annotations.Mustache
import com.twitter.inject.conversions.map._
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton

@Singleton
private[finatra] class MustacheTemplateLookup {
  private[this] val classToTemplateNameCache =
    new ConcurrentHashMap[Class[_], MustacheTemplate]()

  /* Public */

  def getTemplate(obj: Any): MustacheTemplate = {
    obj match {
      case component: MustacheBodyComponent =>
        if (component.templateName.isEmpty) {
          lookupViaAnnotation(component.data).copy(contentType = component.contentType)
        } else {
          MustacheTemplate(component.contentType, component.templateName)
        }
      case _ => lookupViaAnnotation(obj)
    }
  }

  /* Private */

  @throws[IllegalArgumentException]
  private def lookupViaAnnotation(obj: Any): MustacheTemplate = {
    classToTemplateNameCache.atomicGetOrElseUpdate(obj.getClass, {
      val mustacheAnnotation = obj.getClass.getAnnotation(classOf[Mustache])

      if (mustacheAnnotation == null) {
        throw new IllegalArgumentException(s"Object ${obj.getClass.getCanonicalName} has no Mustache annotation")
      }

      MustacheTemplate(
        contentType = mustacheAnnotation.contentType,
        name = mustacheAnnotation.value + ".mustache"
      )
    })
  }

}
