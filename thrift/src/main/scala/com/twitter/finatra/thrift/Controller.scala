package com.twitter.finatra.thrift

import com.twitter.finagle.Service
import com.twitter.finagle.thrift.ToThriftService
import com.twitter.finatra.thrift.internal.ThriftMethodService
import com.twitter.inject.Logging
import com.twitter.scrooge.ThriftMethod
import scala.collection.mutable.ListBuffer

abstract class Controller extends Logging { self: ToThriftService =>
  private[thrift] val methods = new ListBuffer[ThriftMethodService[_, _]]

  protected def handle[Args, Success](
    method: ThriftMethod
  )(
    f: method.FunctionType
  )(
    implicit argsEv: =:=[Args, method.Args],
    successEv: =:=[Success, method.SuccessType],
    serviceFnEv: =:=[method.ServiceIfaceServiceType, Service[Args, Success]]
  ): ThriftMethodService[Args, Success] = {
    val service: method.ServiceIfaceServiceType = method.toServiceIfaceService(f)
    val thriftMethodService =
      new ThriftMethodService[Args, Success](method, service)

    methods += thriftMethodService
    thriftMethodService
  }
}
