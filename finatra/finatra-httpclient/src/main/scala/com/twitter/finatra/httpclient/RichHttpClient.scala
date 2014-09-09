package com.twitter.finatra.httpclient

import com.twitter.finagle._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

object RichHttpClient {

  /* Public */

  def newClientService(target: String) = {
    createService(
      Http.newClient(target))
  }

  def nettyToFinagleHttp(nettyService: Service[HttpRequest, HttpResponse]): Service[Request, Response] = {
    new Service[Request, Response] {
      def apply(request: Request): Future[Response] = {
        nettyService.apply(request) map Response.apply
      }
    }
  }

  /* Private */

  private def createService(serviceFactory: ServiceFactory[HttpRequest, HttpResponse]): Service[Request, Response] = {
    nettyToFinagleHttp(
      serviceFactory.toService)
  }
}
