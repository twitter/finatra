package com.twitter.finatra.httpclient

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service}

object RichHttpClient {

  /* Public */

  def newClientService(dest: String): Service[Request, Response] = {
    Http.newClient(dest).toService
  }

  def newSslClientService(sslHostname: String, dest: String): Service[Request, Response] = {
    Http.client.withTls(sslHostname).newService(dest)
  }
}
