package com.twitter.inject.thrift.integration.reqrepserviceperendpoint

import com.twitter.finagle.{Filter, Service}
import com.twitter.greeter.thriftscala.Greeter.Bye
import com.twitter.inject.Logging
import com.twitter.scrooge
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class ByeFilter
    extends Filter[scrooge.Request[Bye.Args], scrooge.Response[Bye.SuccessType], scrooge.Request[
      Bye.Args
    ], scrooge.Response[Bye.SuccessType]]
    with Logging {
  def apply(
    request: scrooge.Request[Bye.Args],
    service: Service[scrooge.Request[Bye.Args], scrooge.Response[Bye.SuccessType]]
  ): Future[scrooge.Response[Bye.SuccessType]] = {
    info("Bye called with name " + request.args.name)
    service(request)
  }
}
