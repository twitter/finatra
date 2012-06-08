package com.posterous.finatra

import org.jboss.netty.handler.codec.http._
import scala.collection.JavaConversions._
import com.capotej.finatra_core.FinatraCookie

object FinatraCookieAdapter {
  def apply(c: Cookie): FinatraCookie = {
    new FinatraCookie(
      expires=c.getMaxAge,
      comment=c.getComment,
      commentUrl=c.getCommentUrl,
      domain=c.getDomain,
      path=c.getPath,
      ports=c.getPorts,
      name=c.getName,
      version=c.getVersion,
      isDiscard=c.isDiscard,
      isHttpOnly=c.isHttpOnly,
      isSecure=c.isSecure,
      value=c.getValue
    )
  }
}