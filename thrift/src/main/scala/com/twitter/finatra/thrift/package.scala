package com.twitter.finatra

import com.twitter.finagle.Service
import com.twitter.scrooge.{Request, Response}

package object thrift {
  private[thrift] type ScroogeServiceImpl = Service[Request[_], Response[_]]
}
