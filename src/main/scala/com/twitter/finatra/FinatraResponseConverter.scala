package com.twitter.finatra

import com.twitter.finatra_core.ResponseConverter
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http._

class FinatraResponseConverter extends ResponseConverter[Response, Future[HttpResponse]] {
  override def apply(resp: Response) = {
    resp.build
  }
}
