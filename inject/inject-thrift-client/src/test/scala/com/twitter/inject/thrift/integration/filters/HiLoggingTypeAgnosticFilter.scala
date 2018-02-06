package com.twitter.inject.thrift.integration.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.greeter.thriftscala.Greeter.Hi
import com.twitter.inject.Logging
import com.twitter.scrooge
import com.twitter.util.Future

class HiLoggingTypeAgnosticFilter extends Filter.TypeAgnostic with Logging {

  def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new Filter[Req, Rep, Req, Rep] {
    def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
      request match {
        case _: scrooge.Request[_] =>
          info("Hi called with name " + request.asInstanceOf[scrooge.Request[Hi.Args]].args.name)
        case _ =>
          info("Hi called with name " + request.asInstanceOf[Hi.Args].name)
      }

      service(request)
    }
  }
}
