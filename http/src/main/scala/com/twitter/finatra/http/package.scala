package com.twitter.finatra

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Method, Response, Request}

package object http {

  private[http] val AnyMethod = Method("ANY")

  private[http] type HttpFilter = Filter[Request, Response, Request, Response]
}
