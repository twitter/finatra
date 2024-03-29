package com.twitter.inject.thrift.integration.filters

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.scrooge
import com.twitter.test.thriftscala.EchoService.SetTimesToEcho
import com.twitter.util.Future
import com.twitter.util.logging.Logging

class SetTimesEchoTypeAgnosticFilter extends Filter.TypeAgnostic with Logging {

  def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new Filter[Req, Rep, Req, Rep] {
    def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
      request match {
        case _: scrooge.Request[_] =>
          info(
            "SetTimesToEcho called with times " + request
              .asInstanceOf[scrooge.Request[SetTimesToEcho.Args]].args.times)
        case _ =>
          info(
            "SetTimesToEcho called with times " + request.asInstanceOf[SetTimesToEcho.Args].times)
      }

      service(request)
    }
  }
}
