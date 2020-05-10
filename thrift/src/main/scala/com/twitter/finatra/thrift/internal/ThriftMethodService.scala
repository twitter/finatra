package com.twitter.finatra.thrift.internal

import com.twitter.finagle
import com.twitter.finagle.param
import com.twitter.finagle.{Service, ServiceFactory, Stack}
import com.twitter.scrooge.ThriftMethod
import com.twitter.util.Future

private[thrift] class ThriftMethodService[Args, Result](
  val method: ThriftMethod,
  val service: Service[Args, Result])
    extends Service[Args, Result] {

  private[this] val leaf: Stack[ServiceFactory[Args, Result]] =
    Stack.leaf(finagle.stack.Endpoint, ServiceFactory.const(service))

  private[this] var stack: Stack[ServiceFactory[Args, Result]] = leaf

  private[this] lazy val filteredService: Service[Args, Result] = {
    val params = Stack.Params.empty + param.Tags(method.name, method.serviceName)
    Service.pending(stack.make(params)())
  }

  private[finatra] def name: String = method.name

  private[finatra] def setStack(newStack: Stack[ServiceFactory[Args, Result]]): Unit =
    stack = newStack ++ leaf

  def apply(request: Args): Future[Result] =
    filteredService(request)
}
