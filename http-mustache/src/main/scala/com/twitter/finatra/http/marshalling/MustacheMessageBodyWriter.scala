package com.twitter.finatra.http.marshalling

import com.twitter.finatra.mustache.marshalling.MustacheService
import javax.inject.{Inject, Singleton}

@Singleton
class MustacheMessageBodyWriter @Inject() (
  mustacheService: MustacheService,
  templateLookup: MustacheTemplateLookup)
    extends MessageBodyWriter[Any] {

  /* Public */

  override def write(obj: Any): WriterResponse = {
    val template = templateLookup.getTemplate(obj)

    WriterResponse(
      template.contentType,
      mustacheService.createBuffer(template.name, getScope(obj))
    )
  }

  /* Private */

  private def getScope(obj: Any): Any = {
    obj match {
      case c: MustacheBodyComponent => c.data
      case _ => obj
    }
  }

}
