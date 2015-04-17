package com.twitter.finatra.httpclient

import com.twitter.finagle._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

object RichHttpClient {

  /* Public */

  def newClientService(target: String) = {
    nettyToFinagleHttp(
      Http.newClient(target).toService)
  }

  def newSslClientService(sslHostname: String, target: String) = {
    nettyToFinagleHttp(
      Http.client.withTls(sslHostname).newService(target))
  }

  def nettyToFinagleHttp(nettyService: Service[HttpRequest, HttpResponse]): Service[Request, Response] = {
    new Service[Request, Response] {
      def apply(request: Request): Future[Response] = {
        nettyService.apply(request) map Response.apply
      }
    }
  }
}
