package com.twitter.finatra.streams.transformer.domain

//TODO: Rename resultState to WindowResultType
case class WindowedValue[V](resultState: WindowResultType, value: V) {

  def map[VV](f: V => VV): WindowedValue[VV] = {
    copy(value = f(value))
  }
}
