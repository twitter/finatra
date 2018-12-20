package com.twitter.finatra.streams.thriftscala

object WindowResultType {
  @deprecated("Use com.twitter.finatra.streams.transformer.domain.WindowClosed")
  object WindowClosed
      extends com.twitter.finatra.streams.transformer.domain.WindowResultType(
        com.twitter.finatra.streams.transformer.domain.WindowClosed.value) {

    override def toString: String = "WindowClosed"
  }
}
