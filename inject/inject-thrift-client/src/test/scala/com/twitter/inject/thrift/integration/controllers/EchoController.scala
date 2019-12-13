package com.twitter.inject.thrift.integration.controllers

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.thrift.Controller
import com.twitter.scrooge.Response
import com.twitter.test.thriftscala.EchoService
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

@Singleton
class EchoController @Inject()(stats: StatsReceiver)
  extends Controller(EchoService) {

  handle(EchoService.Echo).withFn { req =>
    Future.value(
      Response(req.args.msg)
    )
  }

  handle(EchoService.SetTimesToEcho) { req =>
    Future.value(
      req.times
    )
  }

}

