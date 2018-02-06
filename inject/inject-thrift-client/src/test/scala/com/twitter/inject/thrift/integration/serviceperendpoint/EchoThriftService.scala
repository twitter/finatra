package com.twitter.inject.thrift.integration.serviceperendpoint

import com.twitter.finagle.Service
import com.twitter.inject.Logging
import com.twitter.inject.thrift.integration.AbstractThriftService
import com.twitter.test.thriftscala.EchoService
import com.twitter.test.thriftscala.EchoService.{Echo, SetTimesToEcho}
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger

class EchoThriftService(
  clientId: String
) extends AbstractThriftService
  with EchoService.ServicePerEndpoint
  with Logging {

  private val timesToEcho = new AtomicInteger(1)

  /* Public */

  def echo: Service[Echo.Args, Echo.SuccessType] = Service.mk { args: Echo.Args =>
    info("echo " + args.msg)
    assertClientId(clientId)
    Future.value(args.msg * timesToEcho.get)
  }

  def setTimesToEcho: Service[SetTimesToEcho.Args, SetTimesToEcho.SuccessType] = Service.mk { args: SetTimesToEcho.Args =>
    info("setTimesToEcho " + args.times)
    assertClientId(clientId)
    timesToEcho.set(args.times) //mutation
    Future.value(args.times)
  }
}
