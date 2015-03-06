package com.twitter.inject.requestscope

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import javax.inject.Inject

class RequestSchemaRecordFilter @Inject()(
  requestScope: FinagleRequestScope)
  extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    requestScope.seed[Request.Schema.Record](request.ctx)
    service(request)
  }
}
