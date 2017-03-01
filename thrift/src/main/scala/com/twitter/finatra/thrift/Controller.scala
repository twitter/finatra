package com.twitter.finatra.thrift

import com.twitter.finagle.Service
import com.twitter.finatra.thrift.internal.ThriftMethodService
import com.twitter.inject.Logging
import com.twitter.scrooge.{ToThriftService, ThriftMethod}
import scala.collection.mutable.ListBuffer

trait Controller extends Logging { self: ToThriftService =>
  private[thrift] val methods = new ListBuffer[ThriftMethodService[_, _]]

  protected def handle[Args, Result](method: ThriftMethod)(f: method.FunctionType)(
    implicit argsEv: =:=[Args, method.Args],
    resultEv: =:=[Result, method.Result],
    svcEv: =:=[method.ServiceType, Service[Args, Result]]): ThriftMethodService[Args, Result] = {
    val thriftMethodService =
      new ThriftMethodService[Args, Result](method, method.functionToService(f))
    methods += thriftMethodService
    thriftMethodService
  }
}
