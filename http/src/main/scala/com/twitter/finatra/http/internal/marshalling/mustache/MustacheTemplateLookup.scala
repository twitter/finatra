package com.twitter.finatra.http.internal.marshalling.mustache

import com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent
import com.twitter.finatra.http.response.Mustache
import com.twitter.inject.conversions.map._
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton

private[finatra] case class MustacheTemplate(
  contentType: String,
  name: String
)

@Singleton
private[finatra] class MustacheTemplateLookup {

  private val classToTemplateNameCache = new ConcurrentHashMap[Class[_], MustacheTemplate]()

  /* Public */

  def getTemplate(obj: Any): MustacheTemplate = {
    obj match {
      case MustacheBodyComponent(data, templateName, contentType) =>
        if (templateName.isEmpty) {
          lookupViaAnnotation(data).copy(contentType = contentType)
        } else {
          MustacheTemplate(contentType, templateName)
        }
      case _ => lookupViaAnnotation(obj)
    }
  }

  /* Private */

  /**
    * @throws IllegalArgumentException if obj does not have [[com.twitter.finatra.http.response.Mustache]] annotation
    */
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
