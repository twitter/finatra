package com.twitter.finatra

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Response, Request}

package object http {

  private[http] type HttpFilter = Filter[Request, Response, Request, Response]
}
