package com.twitter.finatra.httpclient

import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finagle.{Httpx, Service}

object RichHttpClient {

  /* Public */

  def newClientService(dest: String): Service[Request, Response] = {
    Httpx.newClient(dest).toService
  }

  def newSslClientService(sslHostname: String, dest: String): Service[Request, Response] = {
    Httpx.client.withTls(sslHostname).newService(dest)
  }
}
