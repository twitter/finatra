package com.twitter.unittests.integration.compositesum

import com.twitter.finatra.streams.transformer.domain.CompositeKey

object SampleCompositeKey {
  def RangeStart: SampleCompositeKey = SampleCompositeKey(0, 0)
}

case class SampleCompositeKey(primary: Int, secondary: Int) extends CompositeKey[Int, Int]


