package com.twitter.finatra.thrift.codegen

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.finatra.thrift.internal.{ThriftRequestUnwrapFilter, ThriftRequestWrapFilter}
import com.twitter.scrooge.ThriftMethod

case class MethodFilters(
  statsReceiver: StatsReceiver,
  commonFilter: ThriftFilter) {

  def create[T](
    thriftMethod: ThriftMethod)(
    service: Service[T, thriftMethod.SuccessType]): Service[T, thriftMethod.SuccessType] = {

    new ThriftRequestWrapFilter[T, thriftMethod.SuccessType](thriftMethod.name) andThen
      commonFilter.toFilter[T, thriftMethod.SuccessType] andThen
      new ThriftRequestUnwrapFilter[T, thriftMethod.SuccessType] andThen
      service
  }
}
