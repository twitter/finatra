package com.twitter.finatra.httpclient

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service}

@deprecated("Use com.twitter.finatra.httpclient.modules.HttpClientModuleTrait " +
  "or configure the com.twitter.finagle.Http.Client directly.","07-09-2019")
object RichHttpClient {

  /* Public */

  def newClientService(dest: String): Service[Request, Response] = {
    Http.client.newService(dest)
  }

  def newSslClientService(sslHostname: String, dest: String): Service[Request, Response] = {
    Http.client.withTls(sslHostname).newService(dest)
  }
}
