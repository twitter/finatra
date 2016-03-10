package com.twitter.finatra.http.internal.marshalling.mustache

import com.google.common.net.MediaType
import com.twitter.finatra.http.marshalling.mustache.{MustacheBodyComponent, MustacheService}
import com.twitter.finatra.http.marshalling.{MessageBodyWriter, WriterResponse}
import javax.inject.{Inject, Singleton}

@Singleton
class MustacheMessageBodyWriter @Inject()(
  mustacheService: MustacheService,
  templateLookup: MustacheTemplateNameLookup)
  extends MessageBodyWriter[Any] {

  /* Public */

  override def write(obj: Any): WriterResponse = {
    WriterResponse(
      MediaType.HTML_UTF_8,
      mustacheService.createBuffer(
        templateLookup.getTemplateName(obj),
        getScope(obj))
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
