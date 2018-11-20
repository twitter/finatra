package com.twitter.finatra.thrift.internal

import com.twitter.finagle.{Filter, Service}
import com.twitter.scrooge.ThriftMethod
import com.twitter.util.Future

class ThriftMethodService[Args, Result](
  val method: ThriftMethod,
  val service: Service[Args, Result]
) extends Service[Args, Result] {

  private[this] var filter: Filter[Args, Result, Args, Result] = Filter.identity

  def name: String = method.name

  override def apply(request: Args): Future[Result] =
    filter.andThen(service)(request)

  def setFilter(f: Filter.TypeAgnostic): Unit =
    filter = filter.andThen(f.toFilter[Args, Result])
}
