package com.twitter.inject.thrift.internal

import javax.inject.Provider

import com.twitter.finagle.Filter
import com.twitter.inject.thrift.ThriftClientModule.ThriftClientFilter
import scala.collection.mutable.ArrayBuffer

case class ThriftClientFilterChain(
  filterProviders: Seq[Provider[ThriftClientFilter]],
  methodFiltersProviders: Map[String, Seq[Provider[ThriftClientFilter]]]) {

  lazy val filter: ThriftClientFilter = {
    combineFilterProviders(filterProviders)
  }

  lazy val methodFilters: Map[String, ThriftClientFilter] = {
    methodFiltersProviders.mapValues(combineFilterProviders)
  }

  def combineFilterProviders(providers: Seq[Provider[ThriftClientFilter]]) = {
    providers map {_.get} reduce {_ andThen _}
  }
}